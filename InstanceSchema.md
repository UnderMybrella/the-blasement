# League Instance Schematic

The league instance configuration schematic is a complicated set of data, but let's try and break it down, including with examples.

NOTE: Explicit 'nulls' are treated as 'service unsupported' for endpoints.

## league_id
The instance id for this league.

## visibility
The visibility of this league. Must be public, protected, private, or null. Defaults to Public

## clock
The time source of this league. Must be either a string, or an object.

When clock is a string, it must either be `UTC` or an instant timestamp, such as `2020-10-18T00:33:37.014Z`.
If it is an instant timestamp, it is treated as an unbounded time source at a real time rate of acceleration.

When clock is an object, it requires a property to define the time source.

### clock.type = "static"
This instance uses a static clock, locked into a single point in time. The time is parsed from `clock.time` as an instant timestamp.

This example is a static clock set right before Season 10, Day X:
```json
{
  "clock": {
    "type": "static",
    "time": "2020-10-18T00:33:37.014Z"
  }
}
```

### clock.type = "unbounded"
This instance uses an unbounded clock, starting at a point in time and running forever, at a certain pace.

Properties:
- The starting point in time is parsed from `clock.time`
- The acceleration rate is parsed from `clock.acceleration_rate` (defaults to 1.0),
- The temporal update rate (How often we update the websocket) is parsed as seconds from `clock.temporal_update_rate` (Defaults to 1 seconds)
- The event stream update rate (How often is data pushed through the SSE endpoint) is parsed as seconds from `clock.event_stream_update_rate` (Defaults to 4 seconds)

This example is an unbounded clock set right before Season 10, Day X, which:
- Runs at 2.5x speed
- Updates the time websocket every second
- Pushes data through the SSE endpoint every second
```json
{
  "clock": {
    "type": "unbounded",
    "time": "2020-10-18T00:33:37.014Z",
    "acceleration_rate": 2.5,
    "temporal_update_rate": 1.0,
    "event_stream_update_rate": 1.0
  }
}
```

### clock.type = "bounded"
This instance uses a bounded clock, starting at a point in time and running until another point in time, at which point it will loop back to the start.

Properties:
- The starting point in time is parsed from `clock.time`
- The ending point in time is parsed from `clock.end`
- The acceleration rate is parsed from `clock.acceleration_rate` (defaults to 1.0),
- The temporal update rate (How often we update the websocket) is parsed as seconds from `clock.temporal_update_rate` (Defaults to 1 seconds)
- The event stream update rate (How often is data pushed through the SSE endpoint) is parsed as seconds from `clock.event_stream_update_rate` (Defaults to 4 seconds)

This example is a bounded clock set right before Season 10, Day X, which:
- Ends after the boss fight
- Runs at 5x speed
- Updates the time websocket every half second
- Pushes data through the SSE endpoint every second
```json
{
  "clock": {
    "type": "unbounded",
    "time": "2020-10-18T00:33:37.014Z",
    "end": "2020-10-18T02:11:00Z",
    "acceleration_rate": 5.0,
    "temporal_update_rate": 0.5,
    "event_stream_update_rate": 1.0
  }
}
```

### clock.type = "clock"
This instance uses a realtime clock.

Properties:
- The temporal update rate (How often we update the websocket) is parsed as seconds from `clock.temporal_update_rate` (Defaults to 1 seconds)
- The event stream update rate (How often is data pushed through the SSE endpoint) is parsed as seconds from `clock.event_stream_update_rate` (Defaults to 4 seconds)

This example is a realtime clock, with the following properties:
- Updates the time websocket every half second
- Pushes data through the SSE endpoint every second
```json
{
  "clock": {
    "type": "clock",
    "temporal_update_rate": 0.5,
    "event_stream_update_rate": 1.0
  }
}
```

## siteDataClock
The time source to use for site data files. See [clock] to find the schema.

## apiGetActiveBets
Active bets for the current authenticated user. Must be either a string, or an object.

When it's a string, must be "empty", which returns empty data.
When clock is an object, it requires a property to define the endpoint.

### apiGetActiveBets.type = "empty"
Returns an empty array

### apiGetActiveBets.type = "static"
Returns a static set of data, parsed from `apiGetActiveBets.data`.

Example:
```json
{
  "apiGetActiveBets": {
    "type": "static",
    "data": [
      {
        "id": "c82f1a92-8d1b-44ee-9638-723f909c0a74",
        "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
        "rewardId": "3e25720d-7022-4d5f-a4d6-e8d5d400d10c",
        "created": "2021-07-19T15:05:37.842Z",
        "type": 0,
        "amount": 1600,
        "payout": 3179,
        "targets": [
          "b024e975-1c4a-4575-8936-a3754a08806a",
          "c18c4adf-ef33-4d14-8ebb-389bd4b11655"
        ],
        "resolved": true
      }
    ]
  }
}
```