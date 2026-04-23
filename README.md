# Identity Platform

Identity Platform is a multi-tenant OAuth 2.0 and OpenID Connect platform built around a simple split:

- platform users own and operate the identity layer
- client users sign in to the apps those platform users register

This repo contains the full system: a Spring Authorization Server backend, a React control panel frontend, Nginx routing, and Docker deployment wiring. The project handles registered-client lifecycle, tenant-aware user management, JWT issuance, token auditing, subscription-aware limits, Stripe billing, and signing-key rotation in one codebase.

## Why This Exists

Identity Platform was built around a problem that feels more important now than it did even a few years ago: identity is not just a login screen anymore.

As more systems get built and touched by automation, weak ownership boundaries, poor token traceability, and shallow audit visibility become real security problems. This project was built with those concerns in mind from the start.

The goal was not just to support OAuth 2.0 and OpenID Connect, but to build a system where you can tell who owns what, which client a user belongs to, what key signed a token, and what happened when something starts to look wrong.

## What It Does

- registers OAuth and OIDC clients at runtime instead of hardcoding them
- keeps platform admins separate from end users
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

That separation was deliberate. Hibernate is useful, but it can start driving behavior in ways I did not want once too much depends on session state, lazy loading, or whether code happens to be inside a transaction.
It keeps the business model separate from ORM behavior, but it also means the mapping layer has to stay disciplined or it can start loading more than a caller actually needs.

## Best Fit

Identity Platform is a better fit for applications that keep a real user base in a database and need visibility into user activity, token lifecycle, tenant boundaries, and system behavior.

It makes more sense for systems where identity is part of how the app works and part of how it is secured, not just something added at the edge.

### Frontend Modeling

The frontend follows the same general idea: shape and rules are modeled explicitly instead of being scattered across components.

It is not a fully TypeScript-only frontend, but the flows with the most structure, especially registered-client management, are modeled deliberately. Field definitions, validation messages, endpoint configuration, and auth or subscription state are kept centralized so those flows do not turn into page-by-page logic.

### Token Hashing

Raw token values are not stored in the database.

Before token values are persisted, they are HMAC-SHA256 hashed with a configured pepper. When the system needs to look up a token again, it hashes the presented value and searches by that hash.

### Signing Key Rotation and Token-to-Key Traceability

JWTs are signed with RSA keys stored in persistence. The signing store guarantees there is always an active signing key, rotates keys on a schedule, and keeps older public keys available in JWKS during rollover so existing tokens can still verify.

Each stored token record also keeps the `kid` that signed it. That means the platform can answer two important questions at the same time:

- which key is currently signing new tokens
- which key signed a token you are looking at in the audit UI

The result is a better key lifecycle than "generate one key and forget it."

### Token Auditing in the Frontend

The frontend is basically the control panel for the auth system.

The registry and user-detail flows let a platform user inspect attached client users, open a user record, see issued tokens, and invalidate one token or all tokens for a client. Recent login and signup activity is pushed into Redis and exposed back to the dashboard so the frontend can show short-lived activity without treating the browser as the source of truth.

### Subscription Tiers

Platform users carry a tier model with allowances for:

- registered clients
- total users across owned clients
- global scopes
- global authorities

The frontend surfaces those limits as usage cards and plan screens. The backend validates them before client creation or client updates are allowed to go through, so plan enforcement does not depend on browser-side checks.

Billing is Stripe-backed, with checkout, plan changes, webhook sync, and pinned demo tiers handled in the same flow.

## Current Direction

The core platform is usable now. The current rollout is less about basic app completion and more about pushing further into security and control features before a full release.

That includes deeper work around suspicious-user tracing, stronger token and signing-key lifecycle controls, IP and device activity visibility, broader admin workflows, and better response paths for things like compromised keys or malicious behavior.

## Frontend

The frontend is an admin UI, not a marketing shell. It is designed around day-to-day identity management:
dashboard view for tier usage and recent activity, registry page for registered clients and attached users, client editor for auth and token settings, client user detail for state changes and token inspection, subscription view for billing changes, and docs inside the app that explain the platform model.

From a product point of view, the React app is how you manage the auth server.

## Security Notes

A few security decisions are worth calling out directly:
passwords are Argon2-hashed, tokens are stored as HMACed values instead of raw secrets, JWT signing keys rotate and stay traceable by `kid`, ownership checks live on the backend, paid-only actions are protected by server-side authorization rules, inactive keys stay available for verification during rollover, and frontend session expiry forces logout and clears cached session artifacts.

## Testing

The backend test suite covers the parts of the system that matter, not just controller happy paths. That includes security flow integration, platform API ownership rules, token invalidation behavior, refresh-token rotation, client registration validation, authorization persistence, and signing-key rotation.
