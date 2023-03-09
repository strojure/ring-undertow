# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## `1.0.97-SNAPSHOT`

Release date `UNRELEASED`



## `1.0.96`

Release date `2023-03-09`

- (deps): Upgrade undertow "1.0.92", zmap "1.3.26".

## `1.0.92`

Release date `2023-03-09`

- (chore): Change license to Unlicense.

## `1.0.86`

Release date `2023-03-03`

- \[ impl ] - Handle nil :body - Add type hint to fix reflection warning.
- \[ test ] - Add tests for request helper functions.
- \[ test ] - Test async handler timeout.

## `1.0.81`

Release date `2023-03-02`

- \[ impl ] - Implement timeout for async ring handler.
- \[ impl ] - Add type hint to `server/start`.
- \[ impl ] - Handle nil :body.
- \[ session ] - Overwrite ring session data accordingly to spec.
- \[ session ] - Handle :recreate meta, unbind session on delete.
- \[ test ] - Add ring request tests.
- \[ test ] - Add ring response tests.
- \[ test ] - Add ring session tests.
- \[ test ] - Add server tests.
- \[ deps ] - Upgrade undertow 1.0.88, zmap 1.3.18.

## `1.0.65-beta10`

Release date `2023-01-23`

- Use `exchange.getRequestURI` for request `:uri`.
- Revert changes incompatible with Ring middlewares:
  > - Accept `HttpHandler` as response instead of handler in the `:body`.
  > - Accept `IBlockingDeref` as response for async execution.

## `1.0.58-beta9`

Release date `2023-01-19`

- ~~Use `exchange.getRequestPath` instead of `.getRequestURI` which can contains
  host.~~
- ~~Accept `HttpHandler` as response instead of handler in the `:body`.~~
- ~~Accept `IBlockingDeref` as response for async execution.~~
- Upgrade dependencies - strojure/undertow "1.0.72-rc2", zmap "1.2.11".

## `1.0.52-beta8`

Release date `2023-01-13`

- Upgrade dependencies - strojure/undertow "1.0.69-rc1", zmap "1.1.5".
- Change `toString` for HeaderMapProxy to use underlying `HeaderMap.toString`.

## `1.0.45-beta7`

Release date `2022-12-25`

- Implement configuration adapter for Luminus ring adapter.
- Upgrade undertow API to version `1.0.64-beta9`

## `1.0.41-beta6`

Release date `2022-12-24`

- Implement `server` namespace for convenient start/stop.
- Rename handler functions:
    - `handler/ring-sync` to `sync`;
    - `handler/ring-async` to `async`.
- Change declarative handler API for consistency with server configuration.

## `1.0.36-beta5`

Release date `2022-12-24`

- Upgrade undertow API to version `1.0.60-beta8`

## `1.0.33-beta4`

Release date `2022-12-23`

- Rename handler functions
    - `handler/sync-ring-handler` to `ring-sync`
    - `handler/async-ring-handler` to `ring-async`
- Upgrade undertow API to version `1.0.57-beta7`
- #1 Implement declarative configuration of ring handler

## `1.0.25-beta.3`

Release date `2022-12-22`

- Upgrade Undertow API to version `1.0.47-beta.6`.

## `1.0.17-beta.2`

Release date `2022-12-21`

- Rename `adapter` namespace to `handler`.
- Add various documentation.
- Upgrade Undertow API.

## `1.0.6-beta1`

Release date `2022-12-20`

- Initial implementation.
