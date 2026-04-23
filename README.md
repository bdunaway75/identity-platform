# Identity Platform

Identity Platform is a multi-tenant OAuth 2.0 and OpenID Connect platform built around a clean split:

- platform users own and operate the identity layer
- client users sign in to the apps those platform users register

This repo contains the full system: a Spring Authorization Server backend, a React control panel frontend, Nginx routing, and Docker deployment wiring. The project handles registered-client lifecycle, tenant-aware user management, JWT issuance, token auditing, subscription-aware limits, Stripe billing, and signing-key rotation in one codebase.

## What It Does

- registers OAuth and OIDC clients at runtime instead of hardcoding them
- keeps platform operators separate from end users
- scopes admin actions to the registered clients a platform user actually owns
- issues JWTs and tracks the signing key used for each token
- stores token values as hashes instead of raw secrets
- lets a platform user inspect and invalidate tokens from the frontend
- tracks recent signups and logins per client
- ties client capacity to subscription tiers
- keeps business models separate from persistence entities instead of letting Hibernate become the domain model

## Repo Layout

- `auth-server/` - Spring Boot authorization server, platform API, hosted login and signup views, billing hooks, persistence
- `frontend/` - React + Vite control panel for dashboard, registry, user detail, subscriptions, and docs
- `docker-compose.yml` - deployment-oriented compose file using published images plus Redis
- `nginx.conf` - routes SPA traffic to the frontend and auth/OIDC paths to the backend

## Core Model

### Platform Users vs Client Users

A platform user is the account that owns the tenant space. Platform users create registered clients, see plan limits, manage client users, inspect tokens, and change subscriptions.

A client user is an end user inside one registered client. Client users are tied to a specific `clientId`, which keeps each app's user base isolated instead of mixing every user into one global login table.

That separation shows up throughout the codebase:

- platform APIs live under `/platform/**`
- admin actions require a platform role and, for paid features, a paid-tier authority
- client user lookups and token actions are filtered against the platform user's owned registered-client ids before anything is returned

### Dynamic Clients Per Platform User

Registered clients are created at runtime and attached to a platform user after validation. A single platform user can own multiple registered clients, and that owned set becomes the boundary for every management action after that.

In practice that means:

- you are not provisioning static tenants by hand
- client registration, updates, redirect URIs, grants, scopes, authorities, and token settings are part of the product workflow
- ownership is enforced on the backend, not trusted to the UI

### Backend Modeling

The backend is modeled as a domain layer, not just a set of JPA entities with controllers attached.

Business logic lives on business models and services. Persistence has its own entity layer. The two are mapped deliberately instead of being treated as the same thing. In this repo that split shows up as:

- `business.model` for the application-facing domain
- `integration.entity` for persistence concerns
- `mapper` classes to translate between the two
- repository gateways and implementations to keep Hibernate details from leaking into the rest of the app

That separation was deliberate. Hibernate is useful, but it can have a mind of its own when application behavior starts depending on session state, lazy loading, or whether code happens to be inside a transaction.

Pros:

- keeps the business model independent of ORM behavior
- makes core logic easier to reason about outside transaction boundaries
- reduces the chance that lazy-loading quirks or entity state accidentally define application behavior
- gives the backend a cleaner place for business rules than "whatever the entity graph currently looks like"

Tradeoffs:

- if the mapper layer is not split by actual data needs, it can become a generic "load and map everything" layer
- once that happens, the mapper may eagerly walk associations the caller does not actually need
- that means you lose some of the optimization Hibernate can give you through proxies and lazy loading
- the architecture gets cleaner conceptually, but it can get less efficient at runtime if the mapping layer is not kept disciplined

### Frontend Modeling

The frontend follows the same general idea: shape and rules are modeled explicitly instead of being scattered across components.

It is not a fully TypeScript-only frontend, but the flows with the most structure, especially registered-client management, are modeled deliberately. In practice that shows up as:

- `src/types` for client DTOs, form shapes, response-like models, and API payloads
- centralized field metadata in `src/constants/RegisteredClientFields.ts`
- centralized validation messages in `src/constants/ClientValidationMessages.ts`
- serializer and normalizer functions in the service layer so pages do not need to understand backend payload quirks directly
- centralized endpoint configuration in `src/config/endpoints.js`
- dedicated auth and subscription modules so session and tier state are not reimplemented page by page

The goal was to keep components focused on rendering and interaction while keeping models, API contracts, validation rules, and state boundaries explicit. That makes the client-registration and user-management flows easier to change without turning every page into its own source of truth.

### Token Hashing

Raw token values are not stored in the database.

Before token values are persisted, they are HMAC-SHA256 hashed with a configured pepper. When the system needs to look up a token again, it hashes the presented value and searches by that hash.

That gives you a few useful properties:

- the auth database is not holding raw bearer tokens
- token audit records can still be matched and invalidated
- the frontend only needs token metadata like subject, type, issue time, expiry, revocation time, and `kid`

### Signing Key Rotation and Token-to-Key Traceability

JWTs are signed with RSA keys stored in persistence. The signing store guarantees there is always an active signing key, rotates keys on a schedule, and keeps older public keys available in JWKS during rollover so existing tokens can still verify.

Each stored token record also keeps the `kid` that signed it. That means the platform can answer two important questions at the same time:

- which key is currently signing new tokens
- which key signed a token you are looking at in the audit UI

The result is a cleaner key lifecycle than "generate one key and forget it."

### Token Auditing in the Frontend

The frontend is basically a command panel for the auth system.

The registry and user-detail flows let a platform user:

- inspect attached client users
- open a user record from the dashboard or registry
- see issued tokens for that user
- invalidate one token directly
- invalidate all tokens for a client from the platform API

Recent login and signup activity is pushed into Redis and exposed back to the dashboard so the frontend can show short-lived operational activity without treating the browser as the source of truth.

### Subscription Tiers

Platform users carry a tier model with allowances for:

- registered clients
- total users across owned clients
- global scopes
- global authorities

The frontend surfaces those limits as usage cards and plan screens. The backend validates them before client creation or client updates are allowed to go through, so plan enforcement does not depend on browser-side checks.

Billing flows are Stripe-backed:

- checkout sessions are created from the platform UI
- upgrade and downgrade requests map to Stripe price ids
- webhooks sync the resolved tier back onto the platform user
- demo accounts can be pinned to a non-changeable tier

## OAuth and OIDC Surface Area

Identity Platform is built around standard OAuth 2.0 and OpenID Connect behavior:

- authorization code flow
- PKCE support for public clients
- JWT access tokens
- OIDC login for the frontend itself
- client-level redirect and post-logout redirect URI validation
- client-level token TTL settings
- optional refresh-token rotation
- consent toggles
- `azp` claim set to the requesting client
- normalized `authorities` claim added to access tokens

The platform API also checks that bearer tokens were minted for the frontend client before allowing access to `/platform/**`.

## Frontend

The frontend is an operator UI, not a marketing shell. It is designed around day-to-day identity management:

- dashboard view for tier usage, recent signups, and recent logins
- registry page for registered clients and attached users
- client editor for auth methods, grant types, scopes, authorities, roles, redirect URIs, and token settings
- client user detail page for user state changes and token inspection
- subscription view for checkout, upgrades, downgrades, and plan comparison
- docs page that explains the platform model inside the app

From a product point of view, the React app is the control surface for the auth server.

## Security Notes

A few security decisions are worth calling out directly:

- passwords are Argon2-hashed
- tokens are stored as HMACed values, not raw secrets
- JWT signing keys rotate and remain traceable by `kid`
- ownership checks live on the backend
- paid-only actions are protected by server-side authorization rules
- inactive keys stay available for verification during rollover, but do not keep signing new tokens
- session expiry in the frontend forces logout and clears cached session artifacts

## Stack

### Backend

- Java 21
- Spring Boot 3.5
- Spring Authorization Server
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Stripe
- MapStruct
- Querydsl
- JSP-based hosted auth views

### Frontend

- React 18
- Vite
- React Router
- Mantine
- Bootstrap
- `oidc-client-ts`

### Infra

- Nginx
- Docker
- Docker Compose

## Testing

The backend test suite covers the parts of the system that matter operationally, not just controller happy paths. That includes security flow integration, platform API ownership rules, token invalidation behavior, refresh-token rotation, client registration validation, authorization persistence, and signing-key rotation.
