# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## `1.0.46-SNAPSHOT`

Release date `UNRELEASED`

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
