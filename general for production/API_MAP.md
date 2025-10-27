# SPCTR Backend API Map

This document summarizes available endpoints, auth requirements, and typical usage for the Spectre backend.

Last updated: 2025-09-14

## Auth Model
- Authentication: JWT access token (HS256) via `Authorization: Bearer <token>`.
- Login provider: Discord OAuth2. Backend exchanges `code` for Discord token and issues a backend JWT.
- Roles: `ROLE_USER`, `ROLE_MEMBER`, `ROLE_ADMIN`.
- Sessions: Stateless; refresh tokens are currently disabled.

## CORS
- Allowed origins: `security.cors.allowed-origins` (default `http://localhost:5173`).

## Rate Limiting
- Global filter per IP using Bucket4j. Grouped buckets by URI prefix. Header: `X-Rate-Limit-Remaining`.

## Error Contract
- `ResponseStatusException` → `{ "error": string, "status": number }` with matching HTTP status.
- Unhandled → `{ "error": "Internal Server Error", "status": 500 }`.
- Error path (whitelabel replacement): `/api/public/error`.

---

## Public
- GET `/api/public/health` → `{ status: "UP" }` — healthcheck.
- GET `/api/public/config` → `{ oauthProvider, discordClientId, loginUrl, swaggerUrl, features{...} }`.
- GET `/api/public/uex/diagnostics?secret=...` → commodity fetch diagnostics; guarded by `app.dev.dev-sync-secret` or fallback `letmein`.

## Authentication
- GET `/api/auth/discord/login` → `{ url }` — OAuth authorize URL (includes a server-side state).
- GET `/api/auth/discord/callback?code=...&state=...` → `{ accessToken }` — issues JWT and creates/updates user.
- POST `/api/auth/logout?userId=<id>` → 204 — clears server-side refresh tokens (if any). No refresh cookie used currently.

## User
- GET `/api/me` (Auth) → `{ id, username, roles }` — returns current user profile.

## Ships
- GET `/api/ships/names` (Auth) → `string[]` — list of ship names (requires login via Discord).
- GET `/api/ships/info?name=<string>` (Auth) → `ShipResponse` — details for the first matching ship (requires login).
- POST `/api/ships/admin/sync` (ADMIN) → `"Synced N ships."` — manual sync from SC Wiki.

## Tools
- POST `/api/tools/compare` (MEMBER/ADMIN) → `{ a: ShipResponse, b: ShipResponse, diff: {...} }` — compare two ships by name.
- GET `/api/tools/commodities?q=<str>` (MEMBER/ADMIN) → `CommoditySummary[]` — best buy/sell per commodity.
- POST `/api/tools/routes` (MEMBER/ADMIN) `{ topN?, quantity?, system?, allowCrossSystem? }` → `RouteProposal[]` — simple route proposals sorted by profit.

## Images
- GET `/api/images?page=&size=` (Public, paged) → `Page<ListItem>` — gallery listing; marks `mine` for current user when authenticated.
- GET `/api/images/{id}/raw` (Public) → binary image with content-type and cache headers.
- POST `/api/images` (USER/MEMBER/ADMIN, multipart) `file`, `title?` → `UploadResponse` — stores JPEG-encoded data server-side.
- DELETE `/api/images/{id}` (Owner or ADMIN) → 204 — removes image.

## Posts
- GET `/api/posts?page=&size=` (Public, paged) → `Page<Response>`.
- GET `/api/posts/{id}` (Public) → `Response`.
- POST `/api/posts` (MEMBER/ADMIN) → `Response` — creates post for current user.
- PUT `/api/posts/{id}` (Owner or ADMIN; MEMBER/ADMIN required) → `Response` — updates post.
- DELETE `/api/posts/{id}` (Owner or ADMIN; MEMBER/ADMIN required) → 204 — deletes post.

## Admin
- GET `/api/admin/users` (ADMIN) → `UserSummary[]` — list users and roles.
- POST `/api/admin/users/{id}/roles` (ADMIN) `{ roles: ["ROLE_USER", ...] }` → `UserSummary` — overwrites roles (ensures `ROLE_USER`).
- POST `/api/admin/commodities/refresh` (ADMIN) → `{ upserts: number }` — triggers UEX refresh.
- GET `/api/admin/commodities/diagnostics` (ADMIN) → metadata for UEX payload.

---

## Entities / DTOs (selected)
- ShipResponse: `{ id, uuid, name, manufacturer, type, focus, size, cargoCapacity, scmSpeed, navMaxSpeed, pitch, yaw, roll, hp, pledgeUrl, description }`
- Image ListItem: `{ id, title, url, uploaderId, uploadedAt, mine }`
- Post Response: `{ id, userId, title, content, createdAt, updatedAt, mine }`
- CommoditySummary: `{ commodity, buyLocation, buyPrice, sellLocation, sellPrice, spread }`
- RouteProposal: `{ commodity, buyLocation, buyPrice, sellLocation, sellPrice, spread, quantity, profit }`

## Notes
- JWT secret must be Base64-encoded (`app.jwt.secret`).
- OAuth redirect URI (local dev): `http://localhost:5173/auth/callback`.
- Compare feature is visible and usable only for `ROLE_MEMBER`/`ROLE_ADMIN`.
