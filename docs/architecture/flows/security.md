# Security Flows

This document details critical workflows for security setup (MFA) and brute-force protection mechanisms within the `backend-platform-sdk`.

## 1. Setup TOTP (MFA Initialization)

Demonstrates how a user initiates TOTP setup. It involves cryptographic generation, saving the encrypted secret provisionally, and issuing an MFA challenge token for the subsequent verification step.

```mermaid
sequenceDiagram
    participant Client
    participant SetupUseCase as SetupTotpUseCase
    participant Crypto as TotpCryptoProcessor
    participant TotpManager
    participant MfaService
    participant Redis
    participant Postgres as DB (Exposed)

    Client->>SetupUseCase: POST /api/v1/user/security/totp/setup
    SetupUseCase->>Redis: checkRateLimit(USER_SETUP_TOTP, userId)
    
    SetupUseCase->>Postgres: dbQuery { getUserByIdForSelf }
    Postgres-->>SetupUseCase: User details

    alt TOTP already enabled
        SetupUseCase-->>Client: 400 Error (TotpAlreadyEnabled)
    else TOTP not enabled
        SetupUseCase->>Crypto: generateNewSecret(accountName)
        Crypto-->>SetupUseCase: GeneratedTotp (decrypted, encrypted, otpAuthUrl)
        
        SetupUseCase->>TotpManager: initiateTotpSetup(userId, encryptedSecret)
        TotpManager->>Postgres: dbQuery { save pending secret }
        
        SetupUseCase->>MfaService: createChallenge(SETUP_TOTP)
        MfaService->>Redis: store mfaToken with TTL
        MfaService-->>SetupUseCase: MfaChallengeData(mfaToken)
        
        SetupUseCase-->>Client: 200 OK (secretKey, otpAuthUrl, mfaToken)
    end
```
*Note: The setup is not complete until the client calls `EnableTotpUseCase` with a valid code derived from the `secretKey` alongside the `mfaToken`.*

## 2. Lockout & Brute-Force Protection

The `LockoutManager` combined with `RateLimiter` secures authentication endpoints against credential stuffing and brute-force attacks. When thresholds are exceeded, the account is temporarily or permanently locked (moving to `SECURITY_HOLD`).

```mermaid
sequenceDiagram
    participant Client
    participant Ktor as LoginByEmailUseCase
    participant RateLimiter
    participant Lockout as LockoutManager
    participant AuthManager
    participant Redis
    participant Postgres as DB (Exposed)

    Client->>Ktor: POST /api/v1/auth/login (email, password)
    
    %% Rate Limiting Step
    Ktor->>RateLimiter: checkRateLimit(LOGIN_ATTEMPT, email)
    RateLimiter->>Redis: incrementWithExpiration(key)
    Redis-->>RateLimiter: current count
    alt Exceeds limit
        RateLimiter-->>Ktor: Error (TooManyRequests)
        Ktor-->>Client: 429 Too Many Requests
    end
    
    %% Hard Lockout Check
    Ktor->>Lockout: isIndefiniteLockout(email)
    Lockout->>Redis: check indefinite key
    Redis-->>Lockout: false

    %% Auth Validation
    Ktor->>AuthManager: authenticateExistingUser(email, password)
    AuthManager->>AuthManager: verify password hash
    
    alt Password Incorrect
        AuthManager-->>Ktor: Error (WrongPassword)
        Ktor->>Lockout: recordFailedAttempt(email, PASSWORD)
        
        Lockout->>Redis: increment failed attempts
        Redis-->>Lockout: attempt count
        
        alt Count exceeds Policy Max Attempts
            Lockout->>Redis: set lockout TTL (Temporary or Indefinite)
            Lockout->>Postgres: dbQuery { lockUserAccount(SECURITY_HOLD) }
            Lockout-->>Ktor: TemporaryLockoutUntil Time
            Ktor-->>Client: 403 User Locked
        else Within limits
            Lockout-->>Ktor: Success (recorded)
            Ktor-->>Client: 400 Wrong Password
        end
    end
```
