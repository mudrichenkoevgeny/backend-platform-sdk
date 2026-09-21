# Authentication Flows

This document details the critical sequence diagrams for user authentication within the `backend-platform-sdk`. The `feature:user` module handles these workflows, enforcing rate limits, MFA challenges, and audit logging.

## 1. Register by Email

Demonstrates the two-step process for registering a new user via email. It validates against domain restrictions, sends a secure OTP, and verifies the OTP to establish the initial session.

```mermaid
sequenceDiagram
    participant Client
    participant SendOTP as SendRegistrationConfirmationToEmailUseCase
    participant Register as RegisterByEmailUseCase
    participant OtpService
    participant EmailPolicy as EmailRestrictionPolicyValidator
    participant AuthManager as AuthManagerImpl
    participant Session as SessionManager
    participant Postgres as DB (Exposed)

    %% Step 1: Request OTP
    Client->>SendOTP: POST /api/v1/auth/register/email/send (email)
    SendOTP->>EmailPolicy: isEmailAllowed(email)
    
    alt Email Domain Blocked
        EmailPolicy-->>SendOTP: false
        SendOTP-->>Client: 400 EmailNotAllowed
    else Email Allowed
        EmailPolicy-->>SendOTP: true
        SendOTP->>OtpService: getOtp(email, EMAIL_VERIFICATION)
        OtpService->>Redis: Generate & store OTP code with TTL
        SendOTP->>EmailService: sendVerificationEmail(email, code)
        SendOTP-->>Client: 200 OK (OtpConfirmation)
    end

    %% Step 2: Submit Registration
    Client->>Register: POST /api/v1/auth/register/email (email, code, password)
    Register->>OtpService: verifyOtp(email, code)
    
    alt Code is incorrect
        OtpService-->>Register: false
        Register-->>Client: 400 WrongConfirmationCode
    else Code is correct
        OtpService-->>Register: true
        Register->>AuthManager: authenticateOrCreateUser(email, password)
        AuthManager->>AuthManager: Hash password (Argon2)
        AuthManager->>Postgres: dbQuery { insert User, insert UserIdentifier }
        
        AuthManager->>Session: createSession()
        Session->>Postgres: dbQuery { insert UserSession }
        Session-->>AuthManager: AuthData (SessionToken, RefreshToken)
        
        AuthManager-->>Register: AuthData
        Register-->>Client: 200 OK (AuthData)
    end
```

## 2. Login by Email (with TOTP MFA)

Demonstrates the flow when a user attempts to log in via email and password, and the account has TOTP enabled.

```mermaid
sequenceDiagram
    participant Client
    participant Ktor as LoginByEmailUseCase
    participant AuthManager as AuthManagerImpl
    participant Lockout as LockoutManager
    participant MfaService as MfaService
    participant Session as SessionManager
    participant Redis
    participant Postgres as DB (Exposed)

    Client->>Ktor: POST /api/v1/auth/login (email, password)
    Ktor->>Redis: checkRateLimit(LOGIN_ATTEMPT, email)
    Ktor->>AuthManager: authenticateExistingUser(email, password)
    
    AuthManager->>Lockout: isIndefiniteLockout(email)
    Lockout->>Redis: check key
    Redis-->>Lockout: false

    AuthManager->>Postgres: dbQuery { get identifier & user }
    Postgres-->>AuthManager: UserDetails + Identifier
    
    AuthManager->>AuthManager: Verify password (Argon2 via PasswordHasher)

    AuthManager->>Postgres: Check if TOTP is enabled
    alt MFA Required
        AuthManager->>MfaService: createChallenge(LOGIN_TOTP)
        MfaService->>Redis: save mfaToken with TTL
        AuthManager-->>Ktor: Error(MfaConfirmationRequired(mfaToken))
        Ktor-->>Client: 401 Unauthorized (mfaToken)
        
        Client->>Ktor: POST /api/v1/auth/login/totp (mfaToken, code)
        Ktor->>Redis: checkRateLimit(LOGIN_ATTEMPT)
        Ktor->>MfaService: validateChallenge(mfaToken)
        MfaService->>Redis: get mfaToken data
        MfaService-->>Ktor: Challenge Data

        Ktor->>Postgres: verifyTotp(userId, code)
        Postgres-->>Ktor: Success

        Ktor->>MfaService: consumeChallenge(mfaToken)
        MfaService->>Redis: delete mfaToken

        Ktor->>AuthManager: completeMfaAuthentication(userId)
    end

    AuthManager->>Session: createSession(userId, clientInfo)
    Session->>Postgres: dbQuery { insert UserSession }
    Session-->>AuthManager: SessionToken + RefreshToken
    AuthManager-->>Ktor: AuthData
    Ktor-->>Client: 200 OK (AuthData)
```

## 3. Login by Phone (OTP SMS)

Demonstrates the passwordless phone login flow. It requires generating an OTP, sending it via SMS, and verifying it before authentication.

```mermaid
sequenceDiagram
    participant Client
    participant SendOTP as SendLoginConfirmationToPhoneUseCase
    participant LoginOTP as LoginByPhoneUseCase
    participant OtpService
    participant PhoneService
    participant AuthManager as AuthManagerImpl
    participant Lockout as LockoutManager
    participant Session as SessionManager
    participant Redis

    %% Step 1: Request OTP
    Client->>SendOTP: POST /api/v1/auth/login/phone/send (phoneNumber)
    SendOTP->>Redis: checkRateLimit(SEND_OTP_PHONE, phoneNumber)
    SendOTP->>OtpService: getOtp(phoneNumber, PHONE_VERIFICATION)
    OtpService->>Redis: Generate and store OTP code with TTL
    OtpService-->>SendOTP: OtpConfirmationData
    SendOTP->>PhoneService: sendVerificationCode(phoneNumber, code, language)
    SendOTP-->>Client: 200 OK (OtpConfirmation)

    %% Step 2: Submit OTP
    Client->>LoginOTP: POST /api/v1/auth/login/phone (phoneNumber, code)
    LoginOTP->>Redis: checkRateLimit(LOGIN_ATTEMPT, phoneNumber)
    LoginOTP->>Lockout: isIndefiniteLockout(phoneNumber)
    LoginOTP->>Lockout: getLockoutUntil(phoneNumber)
    
    LoginOTP->>OtpService: verifyOtp(phoneNumber, code)
    OtpService->>Redis: Retrieve and match OTP
    
    alt Code is incorrect
        OtpService-->>LoginOTP: false
        LoginOTP->>Lockout: recordFailedAttempt(phoneNumber, OTP)
        LoginOTP-->>Client: 400 WrongConfirmationCode (or 403 UserLocked)
    else Code is correct
        OtpService-->>LoginOTP: true
        LoginOTP->>AuthManager: authenticateOrCreateUser(phoneNumber)
        AuthManager->>Session: createSession()
        Session-->>AuthManager: AuthData
        LoginOTP-->>Client: 200 OK (AuthData)
    end
```

## 4. Login by External Provider (OAuth/OIDC)

Handles identity federation via external providers like Google or Apple.

```mermaid
sequenceDiagram
    participant Client
    participant Ktor as LoginByExternalAuthProviderUseCase
    participant Verifier as ExternalAuthVerifier
    participant AuthSettings as AuthSettingsProvider
    participant EmailPolicy as EmailRestrictionPolicyValidator
    participant AuthManager as AuthManagerImpl
    participant Session as SessionManager

    Client->>Ktor: POST /api/v1/auth/login/external (provider, token)
    Ktor->>AuthSettings: getOpenAuthSettings() (Check if provider is enabled)
    Ktor->>Verifier: findVerifier(provider)
    
    Ktor->>Verifier: verify(token)
    Verifier-->>Ktor: VerificationData (externalId, email, provider)
    
    opt If email is provided
        Ktor->>EmailPolicy: isEmailAllowed(email)
        EmailPolicy-->>Ktor: true
    end

    Ktor->>AuthManager: authenticateOrCreateUser(provider, externalId, email)
    AuthManager->>Session: createSession()
    Session-->>AuthManager: AuthData
    Ktor-->>Client: 200 OK (AuthData)
```

## 5. Token Refresh (Session Rotation)

Rotates access and refresh tokens. To prevent brute forcing, rate limiting is applied to the cryptographic hash of the refresh token.

```mermaid
sequenceDiagram
    participant Client
    participant RefreshToken as RefreshTokenUseCase
    participant Provider as RefreshTokenProvider
    participant RateLimiter
    participant Session as SessionManager
    participant Postgres as DB (Exposed)

    Client->>RefreshToken: POST /api/v1/auth/refresh (refreshToken)
    
    RefreshToken->>Provider: getRefreshTokenHash(refreshToken)
    Provider-->>RefreshToken: Hash value
    
    RefreshToken->>RateLimiter: checkRateLimit(REFRESH_TOKEN, hash)
    
    RefreshToken->>Session: refreshSession(refreshToken)
    Session->>Postgres: dbQuery { find session by refreshToken & userId }
    Postgres-->>Session: UserSessionInternal
    
    alt Token Invalid or Expired
        Session-->>RefreshToken: Error(InvalidRefreshToken)
        RefreshToken-->>Client: 401 Unauthorized
    else Token Valid
        Session->>Provider: Generate new SessionToken (Access + Refresh)
        Session->>Postgres: dbQuery { update session with new refresh token }
        Session-->>RefreshToken: SessionToken
        RefreshToken-->>Client: 200 OK (SessionToken)
    end
```