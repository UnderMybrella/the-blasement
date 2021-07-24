# API Layers

Blasement has two levels of APIs -- an API for managing instances, and an API for interacting in those instances (the Blaseball API, with a few tweaks)

The API is hosted at https://blasement.brella.dev.

Before we begin, however, there's another concept we should talk about in terms of visibility, or protection, status.

## Visibility

There are three levels of visibility:

- Public: Anyone can access it, and anyone can see that it's available
- Protected: Anyone can access it if they know the instance ID, nobody can that it's available without the authentication code
- Private: Nobody can access or see that it's available without the authentication code

When creating a league, you'll be required to use an Authorization header, which becomes the authentication code. This is used to later ~~edit~~ (editing is not currently available at this time) or delete an instance.

## Authentication

At some points, you'll be required to pass in an authentication code. The only way to do this at the moment is using the Authorization header.

## Blasement Management

### GET /api/leagues/public

Returns all public leagues as an array. Does not require any authentication.

### GET /api/leagues/protected

Returns all protected leagues that use the provided authentication code.

### GET /api/leagues/private

Returns all private leagues that use the provided authentication code.

### GET /api/leagues/viewable

Returns all leagues that are viewable. Does not *require* an authentication code, but will only return public instances otherwise.

### GET /api/leagues/{instance_id}

Gets the technical description for an instance.

### DELETE /api/leagues/{instance_id}

Deletes a league instance. Requires the authentication code for an instance.

### PUT /api/leagues/{instance_id}

Creates a new league instance. See `POST /api/leagues/new`

### POST /api/leagues/new

Creates a new league instances. Requires an authentication code, which is used for an instance.

The schema for leagues is best described [here](InstanceSchema.md).

## Blaseball League API

(See [SIBR Docs](https://github.com/Society-for-Internet-Blaseball-Research/blaseball-api-spec/))

Full list of implemented endpoints at this time:

- /api/getUser
- /api/getUserRewards
- /api/getActiveBets
- /api/getIdols
- /api/getTribute

- /database/feed/global
- /database/feed/game
- /database/feed/team
- /database/feed/player
- /database/feed/story
- /database/feedByPhase
- /database/globalEvents
- /database/shopSetup
- /database/playerNamesIds
- /database/players
- /database/offseasonSetup
- /database/vault
- /database/sunsun
- /database/allDivisions
- /database/allTeams
- /database/communityChestProgress
- /database/bonusResults
- /database/decreeResults
- /database/eventResults
- /database/gameById/{id}
- /database/getPreviousChamp
- /database/giftProgress
- /database/items
- /database/playersByItemId
- /database/playoffs
- /database/renovationProgress
- /database/renovations
- /database/subleague
- /database/team
- /database/teamElectionStats

- /events/streamData

### GET /api/time

Opens a websocket that receives the current time of the instance.