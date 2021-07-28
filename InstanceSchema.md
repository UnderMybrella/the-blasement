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
The time source to use for site data files. See [clock](#clock) to find the schema.

## apiGetActiveBets
Active bets for the current authenticated user. Must be either a string, or an object.

Defaults to empty data.

When it's a string, must be "empty", which returns empty data.
When it's an object, it requires a property to define the endpoint.

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

## apiGetIdols
Gets the idol leaderboard. Must either be a string, or an object.

Defaults to Chronicler.

When it's a string, must be "chronicler" or "live".
When it's an object, it requires a property to define an endpoint.

### apiGetIdols.type = "chronicler"
Returns data from Chronicler at the league time.

### apiGetIdols.type = "live"
Returns data from the official Blaseball API, time agnostic.

### apiGetIdols.type = "static"
Returns a static set of data, parsed from `apiGetIdols.data`.

Example:
```json
{
  "apiGetIdols": {
    "type": "static",
    "data": {
      "idols": [
        "ef9f8b95-9e73-49cd-be54-60f84858a285",
        "defbc540-a36d-460b-afd8-07da2375ee63",
        "7a75d626-d4fd-474f-a862-473138d8c376",
        "5c983667-6d14-4393-8f15-af904d8f90f8",
        "89f74891-2e25-4b5a-bd99-c95ba3f36aa0",
        "90768354-957e-4b4c-bb6d-eab6bbda0ba3",
        "11de4da3-8208-43ff-a1ff-0b3480a0fbf1",
        "405dfadf-d435-4307-82f6-8eba2287e87a",
        "57a19a22-f2cd-4e59-aa84-15cb0af30ba3",
        "3d293d6c-3a20-43ab-a895-2b7f1f28779f",
        "bc29afc1-c954-4def-978b-a59ae5def3c3",
        "083d09d4-7ed3-4100-b021-8fbe30dd43e8",
        "f2a27a7e-bf04-4d31-86f5-16bfa3addbe7",
        "a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21",
        "bf6a24d1-4e89-4790-a4ba-eeb2870cbf6f",
        "04e14d7b-5021-4250-a3cd-932ba8e0a889",
        "63df8701-1871-4987-87d7-b55d4f1df2e9",
        "678170e4-0688-436d-a02d-c0467f9af8c0",
        "338694b7-6256-4724-86b6-3884299a5d9e",
        "34267632-8c32-4a8b-b5e6-ce1568bb0639"
      ],
      "data": {
        "strictlyConfidential": 9
      }
    }
  }
}
```

## apiGetRisingStars
Gets the upcoming Rising Stars. Must either be a string, or an object.

Defaults to Chronicler.

When it's a string, must be "chronicler" or "live".
When it's an object, it requires a property to define an endpoint.

### apiGetRisingStars.type = "chronicler"
Returns data from Chronicler at the league time.

### apiGetRisingStars.type = "live"
Returns data from the official Blaseball API, time agnostic.

### apiGetRisingStars.type = "static"
Returns a static set of data, parsed from `apiGetRisingStars.data`.

Example:
```json
{
  "apiGetRisingStars": {
    "type": "static",
    "data": {
      "stars": [
        "083d09d4-7ed3-4100-b021-8fbe30dd43e8",
        "836e9395-3f83-4d42-ba11-a12c08ceb78b",
        "7a75d626-d4fd-474f-a862-473138d8c376",
        "405dfadf-d435-4307-82f6-8eba2287e87a",
        "f9fe0130-4741-4103-af9c-86d85724ce54",
        "ef9f8b95-9e73-49cd-be54-60f84858a285",
        "63df8701-1871-4987-87d7-b55d4f1df2e9",
        "89f74891-2e25-4b5a-bd99-c95ba3f36aa0",
        "defbc540-a36d-460b-afd8-07da2375ee63",
        "11de4da3-8208-43ff-a1ff-0b3480a0fbf1",
        "44a22541-7704-403c-b637-3362a76943cb",
        "0bb35615-63f2-4492-80ec-b6b322dc5450",
        "f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1",
        "4e6ad1a1-7c71-49de-8bd5-c286712faf9e",
        "98a60f2f-cf34-4fcb-89aa-2d61fb5fba60",
        "0148c1b0-3b25-4ae1-a7ce-6b1c4f289747",
        "678170e4-0688-436d-a02d-c0467f9af8c0",
        "9e724d9a-92a0-436e-bde1-da0b2af85d8f",
        "bf6a24d1-4e89-4790-a4ba-eeb2870cbf6f",
        "9820f2c5-f9da-4a07-b610-c2dd7bee2ef6",
        "0eddd056-9d72-4804-bd60-53144b785d5c",
        "04e14d7b-5021-4250-a3cd-932ba8e0a889",
        "d0d7b8fe-bad8-481f-978e-cb659304ed49",
        "a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21",
        "a253facf-a54a-493e-b398-cf6f0d288990",
        "5eac7fd9-0d19-4bf4-a013-994acc0c40c0",
        "b643a520-af38-42e3-8f7b-f660e52facc9",
        "b5204124-6f90-46f6-bccc-b1ac11056cca",
        "f2a27a7e-bf04-4d31-86f5-16bfa3addbe7",
        "b082ca6e-eb11-4eab-8d6a-30f8be522ec4",
        "7aeb8e0b-f6fb-4a9e-bba2-335dada5f0a3",
        "62ae6aa9-e346-4faa-b07c-1f7623580015",
        "34267632-8c32-4a8b-b5e6-ce1568bb0639",
        "4f615ee3-4615-4033-972f-79200f9db6e3",
        "88cd6efa-dbf2-4309-aabe-ec1d6f21f98a",
        "57a19a22-f2cd-4e59-aa84-15cb0af30ba3",
        "90c2cec7-0ed5-426a-9de8-754f34d59b39",
        "12c4368d-478b-42be-b6d3-fa2e9b230f82",
        "9a031b9a-16f8-4165-a468-5d0e28a81151"
      ]
    }
  }
}
```

## apiGetTribute
Gets the upcoming Rising Stars. Must either be a string, or an object.

Defaults to Chronicler.

When it's a string, must be "chronicler" or "live".
When it's an object, it requires a property to define an endpoint.

### apiGetTribute.type = "chronicler"
Returns data from Chronicler at the league time.

### apiGetTribute.type = "live"
Returns data from the official Blaseball API, time agnostic.

### apiGetTribute.type = "static"
Returns a static set of data, parsed from `apiGetTribute.data`.

Example:
```json
{
  "apiGetTribute": {
    "type": "static",
    "data": {"players":[{"playerId":"413b3ddb-d933-4567-a60e-6d157480239d","peanuts":67154268},{"playerId":"f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1","peanuts":64863809},{"playerId":"88cd6efa-dbf2-4309-aabe-ec1d6f21f98a","peanuts":63115341},{"playerId":"5eac7fd9-0d19-4bf4-a013-994acc0c40c0","peanuts":60616794},{"playerId":"1068f44b-34a0-42d8-a92e-2be748681a6f","peanuts":59426912},{"playerId":"14d88771-7a96-48aa-ba59-07bae1733e96","peanuts":56951906},{"playerId":"70ccff1e-6b53-40e2-8844-0a28621cb33e","peanuts":55369845},{"playerId":"2e6d4fa9-f930-47bd-971a-dd54a3cf7db1","peanuts":48680338},{"playerId":"c17a4397-4dcc-440e-8c53-d897e971cae9","peanuts":47698635},{"playerId":"8ba7e1ff-4c6d-4963-8e0f-7096d14f4b12","peanuts":44060050},{"playerId":"409d1c96-760b-4a96-9a3c-481112ddf37b","peanuts":42228730},{"playerId":"bca38809-81de-42ff-94e3-1c0ebfb1e797","peanuts":32568304},{"playerId":"43bf6a6d-cc03-4bcf-938d-620e185433e1","peanuts":30189450},{"playerId":"2175cda0-a427-40fd-b497-347edcc1cd61","peanuts":29140420},{"playerId":"c22e3af5-9001-465f-b450-864d7db2b4a0","peanuts":29063240},{"playerId":"06933fdc-a472-4f28-8023-a3661fb6e91f","peanuts":28269269},{"playerId":"69196296-f652-42ff-b2ca-0d9b50bd9b7b","peanuts":27885496},{"playerId":"c18961e9-ef3f-4954-bd6b-9fe01c615186","peanuts":26565317},{"playerId":"0f61d948-4f0c-4550-8410-ae1c7f9f5613","peanuts":26512223},{"playerId":"8d337b47-2a7d-418d-a44e-ef81e401c2ef","peanuts":24215037},{"playerId":"bd9d1d6e-7822-4ad9-bac4-89b8afd8a630","peanuts":24160572},{"playerId":"4f7d7490-7281-4f8f-b62e-37e99a7c46a0","peanuts":20040405},{"playerId":"3d4545ed-6217-4d7a-9c4a-209265eb6404","peanuts":19538453},{"playerId":"34267632-8c32-4a8b-b5e6-ce1568bb0639","peanuts":17180177},{"playerId":"285ce77d-e5cd-4daa-9784-801347140d48","peanuts":16525244},{"playerId":"75f9d874-5e69-438d-900d-a3fcb1d429b3","peanuts":15927617},{"playerId":"a311c089-0df4-46bd-9f5d-8c45c7eb5ae2","peanuts":15750209},{"playerId":"a9812a8e-67c4-434c-85cb-6ddf785cadf2","peanuts":15350849},{"playerId":"25f3a67c-4ed5-45b6-94b1-ce468d3ead21","peanuts":14838834},{"playerId":"ab9eb213-0917-4374-a259-458295045021","peanuts":14015407},{"playerId":"f9c0d3cb-d8be-4f53-94c9-fc53bcbce520","peanuts":13965512},{"playerId":"f8c20693-f439-4a29-a421-05ed92749f10","peanuts":12935537},{"playerId":"425f3f84-bab0-4cf2-91c1-96e78cf5cd02","peanuts":12015038},{"playerId":"1145426a-c1b7-4b50-9073-0528c2f41e18","peanuts":11965745},{"playerId":"4f69e8c2-b2a1-4e98-996a-ccf35ac844c5","peanuts":11875934},{"playerId":"be35caba-b16a-4e0d-b927-4da857f4cdb5","peanuts":11125454},{"playerId":"4f615ee3-4615-4033-972f-79200f9db6e3","peanuts":10527393},{"playerId":"2ae8cbfc-2155-4647-9996-3f2591091baf","peanuts":10406770},{"playerId":"3b218380-d907-43c1-a207-c509bb36b320","peanuts":10051547},{"playerId":"62ae6aa9-e346-4faa-b07c-1f7623580015","peanuts":10008624},{"playerId":"c07ab5a8-ece3-4c4d-b2d2-98e3a7cf864f","peanuts":9378969},{"playerId":"cd68d3a6-7fbc-445d-90f1-970c955e32f4","peanuts":8851169},{"playerId":"2e94fa0d-453f-4925-8ea0-c0a53e38108d","peanuts":8070477},{"playerId":"b28bb7f7-2d8c-4781-8808-83844df7e732","peanuts":7743867},{"playerId":"6ebac746-6685-4d0f-8e84-6b21299b5169","peanuts":7548980},{"playerId":"ab36c776-b520-429b-a85f-bf633d7b081a","peanuts":7201348},{"playerId":"4e6ad1a1-7c71-49de-8bd5-c286712faf9e","peanuts":7096250},{"playerId":"43256f39-9db7-4bda-bc10-814a60b4ede2","peanuts":7057978},{"playerId":"3a8c52d7-4124-4a65-a20d-d51abcbe6540","peanuts":6998602},{"playerId":"1c73f91e-0562-480d-9543-2aab1d5e5acd","peanuts":6943767},{"playerId":"d51f1fe8-4ab8-411e-b836-5bba92984d32","peanuts":6496629},{"playerId":"e2f39815-5291-4dcf-ba19-97dcf0c015e9","peanuts":6450666},{"playerId":"f967d064-0eaf-4445-b225-daed700e044b","peanuts":6235615},{"playerId":"a071a713-a6a1-4b4c-bb3f-45d9fba7a08c","peanuts":6002904},{"playerId":"11f25eae-465f-43cc-9366-f19addc803bc","peanuts":5689001},{"playerId":"22d4c06d-26c2-4031-ae7f-fd0eeb92f57d","peanuts":5682322},{"playerId":"bd8d58b6-f37f-48e6-9919-8e14ec91f92a","peanuts":5674207},{"playerId":"472f50c0-ef98-4d05-91d0-d6359eec3946","peanuts":5655778},{"playerId":"b86237bb-ade6-4b1d-9199-a3cc354118d9","peanuts":5480738},{"playerId":"8e1fd784-99d5-41c1-a6c5-6b947cec6714","peanuts":5255778},{"playerId":"d5b6b11d-3924-4634-bd50-76553f1f162b","peanuts":5180620},{"playerId":"b9293beb-d199-4b46-add9-c02f9362d802","peanuts":5141895},{"playerId":"4ea2afe9-9884-46e5-8bc5-43b720b54556","peanuts":4898370},{"playerId":"f44a8b27-85c1-44de-b129-1b0f60bcb99c","peanuts":4683069},{"playerId":"ecb8d2f5-4ff5-4890-9693-5654e00055f6","peanuts":4523735},{"playerId":"da6f2a92-d109-47ad-8e16-854ae3f88906","peanuts":4366469},{"playerId":"ae9afac3-917a-4d88-9c67-1f7a0b5de8ce","peanuts":4208923},{"playerId":"1b2653a7-602a-4688-83ab-f68e23c79528","peanuts":4118418},{"playerId":"3afb30c1-1b12-466a-968a-5a9a21458c7f","peanuts":4097989},{"playerId":"90c6e6ca-77fc-42b7-94d8-d8afd6d299e5","peanuts":4050208},{"playerId":"33fbfe23-37bd-4e37-a481-a87eadb8192d","peanuts":4009702},{"playerId":"396eacd5-b485-47a7-8891-38dd41a8f58c","peanuts":3825883},{"playerId":"97dfc1f6-ac94-4cdc-b0d5-1cb9f8984aa5","peanuts":3781366},{"playerId":"ef32eb48-4866-49d0-ae58-9c4982e01142","peanuts":3779775},{"playerId":"41949d4d-b151-4f46-8bf7-73119a48fac8","peanuts":3767074},{"playerId":"2b9f9c25-43ec-4f0b-9937-a5aa23be0d9e","peanuts":3618221},{"playerId":"d23a7ba2-6a1f-4c51-8ba7-a35b1d4efa0b","peanuts":3606272},{"playerId":"10ea5d50-ec88-40a0-ab53-c6e11cc1e479","peanuts":3602322},{"playerId":"773712f6-d76d-4caa-8a9b-56fe1d1a5a68","peanuts":3600590},{"playerId":"7b0f91aa-4d66-4362-993d-6ff60f7ce0ef","peanuts":3590357},{"playerId":"15d3a844-df6b-4193-a8f5-9ab129312d8d","peanuts":3480769},{"playerId":"15ae64cd-f698-4b00-9d61-c9fffd037ae2","peanuts":3475027},{"playerId":"535f4e67-a54b-427a-9ca1-1296d7387876","peanuts":3300404},{"playerId":"ce0e57a7-89f5-41ea-80f9-6e649dd54089","peanuts":2708741},{"playerId":"c73d59dd-32a0-49ce-8ab4-b2dbb7dc94ec","peanuts":2624540},{"playerId":"cc11963b-a05b-477b-b154-911dc31960df","peanuts":2588922},{"playerId":"2b1cb8a2-9eba-4fce-85cf-5d997ec45714","peanuts":2143714},{"playerId":"80a2f015-9d40-426b-a4f6-b9911ba3add8","peanuts":2109621},{"playerId":"c83f0fe0-44d1-4342-81e8-944bb38f8e23","peanuts":2002669},{"playerId":"a6fa1c1a-08e8-4617-9a7d-1f91ecf652e3","peanuts":1918000},{"playerId":"5d3c5190-967f-4711-9542-9f03b6978f4a","peanuts":1759803},{"playerId":"a5adc84c-80b8-49e4-9962-8b4ade99a922","peanuts":1755731},{"playerId":"0295c6c2-b33c-47dd-affa-349da7fa1760","peanuts":1750884},{"playerId":"44689655-1b85-4758-aa58-fab2b98144a7","peanuts":1625358},{"playerId":"495a6bdc-174d-4ad6-8d51-9ee88b1c2e4a","peanuts":1621621},{"playerId":"c86b5add-6c9a-40e0-aa43-e4fd7dd4f2c7","peanuts":1617552},{"playerId":"1ba715f2-caa3-44c0-9118-b045ea702a34","peanuts":1560621},{"playerId":"03097200-0d48-4236-a3d2-8bdb153aa8f7","peanuts":1461621},{"playerId":"248ccf3d-d5f6-4b69-83d9-40230ca909cd","peanuts":1356669},{"playerId":"679e0b2f-fabf-4e95-a92d-73df2d9c5c7f","peanuts":1328426},{"playerId":"76c4853b-7fbc-4688-8cda-c5b8de1724e4","peanuts":1250621},{"playerId":"79df0468-f66b-4e25-857e-a6aaff553de6","peanuts":1163021},{"playerId":"c83a13f6-ee66-4b1c-9747-faa67395a6f1","peanuts":1151465},{"playerId":"66c8c4bf-985a-4164-911d-e66df386feb2","peanuts":1106371},{"playerId":"0fe896e1-108c-4ce9-97be-3470dde73c21","peanuts":1102269},{"playerId":"c9d650be-c41b-4a4e-8987-9fea86b0a40a","peanuts":1048203},{"playerId":"38f3ba48-47aa-4116-be5f-91fbcebd82f7","peanuts":1047123},{"playerId":"c7f1d523-5866-40cb-9878-7ee0b8ffbbaf","peanuts":1024000},{"playerId":"262c49c6-8301-487d-8356-747023fa46a9","peanuts":1021615},{"playerId":"8eee96eb-58c9-4ecb-91c8-c461a72f9454","peanuts":1002061},{"playerId":"46bb539b-6af5-4941-bd1c-a5bfdd0ea891","peanuts":899729},{"playerId":"319ca206-58e9-4ac8-b15e-18c2971b26b6","peanuts":891994},{"playerId":"d1a7c13f-8e78-4d2e-9cae-ebf3a5fcdb5d","peanuts":874950},{"playerId":"70a458ed-25ca-4ff8-97fc-21cbf58f2c2a","peanuts":874850},{"playerId":"5b9727f7-6a20-47d2-93d9-779f0a85c4ee","peanuts":874850},{"playerId":"4bda6584-6c21-4185-8895-47d07e8ad0c0","peanuts":836669},{"playerId":"57448b62-f952-40e2-820c-48d8afe0f64d","peanuts":739369},{"playerId":"b4505c48-fc75-4f9e-8419-42b28dcc5273","peanuts":736133},{"playerId":"acfd3082-2fe2-46d2-a00f-231c33f2bdcf","peanuts":730080},{"playerId":"3064c7d6-91cc-4c2a-a433-1ce1aabc1ad4","peanuts":707496},{"playerId":"64b055d1-b691-4e0c-8583-fc08ba663846","peanuts":705191},{"playerId":"2ca0c790-e1d5-4a14-ab3c-e9241c87fc23","peanuts":704435},{"playerId":"bd549bfe-b395-4dc0-8546-5c04c08e24a5","peanuts":638196},{"playerId":"57b9ae39-1471-460b-94c9-75e078d9989e","peanuts":626356},{"playerId":"0bfee9d6-aae4-40b1-9d61-2ffd648ace9d","peanuts":590891},{"playerId":"05bd08d5-7d9f-450b-abfa-1788b8ee8b91","peanuts":583689},{"playerId":"a1cf1286-2b11-46db-ac85-f56818e713b1","peanuts":577820},{"playerId":"cdc59c5c-b62c-43c7-aba9-61df8f59ea74","peanuts":577800},{"playerId":"1f0f2e1e-79b9-4e1b-afe4-1ff8717a0149","peanuts":561200},{"playerId":"b3d518b9-dc68-4902-b68c-0022ceb25aa0","peanuts":553066},{"playerId":"0eea4a48-c84b-4538-97e7-3303671934d2","peanuts":512830},{"playerId":"a8a5cf36-d1a9-47d1-8d22-4a665933a7cc","peanuts":511000},{"playerId":"c9a01692-fc86-49f4-b61c-9bd4cca20303","peanuts":504030},{"playerId":"aac4947f-4101-4fa4-b71c-090b1cad88d3","peanuts":466069},{"playerId":"15b1b9d3-4921-4898-9f71-e33e8e11cae7","peanuts":466069},{"playerId":"a807ea0c-7c58-4f7b-a11b-c6f11f3a1b02","peanuts":437145},{"playerId":"96661094-42a0-49ff-b799-82e7672de95d","peanuts":404685},{"playerId":"4284d9b6-9cab-44e5-a253-1c0fac2bb867","peanuts":264919},{"playerId":"88a988f0-1d3e-4b3d-b51f-8b0e7822620d","peanuts":262520},{"playerId":"44a22541-7704-403c-b637-3362a76943cb","peanuts":237052},{"playerId":"fc34c783-f8fc-4903-8653-d44630fcc368","peanuts":228000},{"playerId":"a23298d1-0008-4938-a13d-857130a522a4","peanuts":222222},{"playerId":"37e74dc8-3d6c-49f5-a4d0-952a9260e7d7","peanuts":218059},{"playerId":"a1325679-cfc0-40fe-b586-e72de2b2f7e3","peanuts":173571},{"playerId":"be043627-28c8-4bf8-9b90-d32a61c1e0ac","peanuts":170000},{"playerId":"22be4191-f541-4a2c-b9d3-5d6528638497","peanuts":154813},{"playerId":"bdde3fcb-1142-4c71-a44b-d912cecc6b00","peanuts":142000},{"playerId":"7c053e11-e9e6-4d40-94a4-f267d6862261","peanuts":115264},{"playerId":"69342c37-cc48-4b57-acbf-36b542980087","peanuts":115000},{"playerId":"c910527c-577d-4653-8add-1cc18bae07ae","peanuts":111999},{"playerId":"2220ba27-a0cc-4bad-86b2-268d087ec243","peanuts":101999},{"playerId":"836e9395-3f83-4d42-ba11-a12c08ceb78b","peanuts":101555},{"playerId":"37543614-6bf0-4062-aa41-e6598136a8dc","peanuts":94111},{"playerId":"6e569544-524a-4428-9ef0-c21e98645517","peanuts":91444},{"playerId":"e5a8901f-177e-486d-86b8-d3aefcd5a68d","peanuts":88555},{"playerId":"21bc2e80-a7ef-4d76-8d6c-651e6a5dcd07","peanuts":86555},{"playerId":"ce24abe4-4d71-4668-b4a6-29342380978a","peanuts":82222},{"playerId":"c793f210-4ec1-421f-afe7-7ee729799624","peanuts":81999},{"playerId":"0a1d221a-f4b2-4691-bb08-c7757659f6c9","peanuts":81999},{"playerId":"dc934118-1b85-4447-8087-9343e388f411","peanuts":80777},{"playerId":"f5a2eb28-ac4e-417f-ac59-c6c4f7c40af2","peanuts":79999},{"playerId":"61569f64-6578-4b6f-b045-7a06e59bcceb","peanuts":79444},{"playerId":"6926d05b-e8eb-48cb-8bb3-21a498e226b2","peanuts":78777},{"playerId":"382d9269-5cd8-4125-a23a-459a640bbaf3","peanuts":77999},{"playerId":"b61b2871-7254-49b3-b009-174a23acaa1a","peanuts":77666},{"playerId":"b234aee4-e9bb-4e82-bb19-49b6846a6f18","peanuts":76444},{"playerId":"abc32ba7-ffe5-484e-9689-ce44f713593d","peanuts":75777},{"playerId":"09847345-5529-4766-9f4a-ed6fefc97b01","peanuts":75444},{"playerId":"c27b80b5-164e-4cb3-bb59-26c87aee79e7","peanuts":75333},{"playerId":"c99dabeb-37f6-403a-aca1-2fc82e4ca631","peanuts":74888},{"playerId":"6d1ccac5-e6f0-48a9-ba4c-e53c01cc93a7","peanuts":74777},{"playerId":"70680e0a-3f31-47cb-8599-acefdf02a4ce","peanuts":74444},{"playerId":"59c92008-4cca-48ef-b5b3-a57000c2b08a","peanuts":74444},{"playerId":"f479b20b-490a-4884-b379-b9a47d117b14","peanuts":73999},{"playerId":"dd730c06-b229-4bdb-a6c4-9d09aa7d5b7d","peanuts":73999},{"playerId":"4da9e7ec-7db9-49ae-a3fb-85f872c0b5ef","peanuts":73999},{"playerId":"4ba50817-d477-438e-af14-824ee6bfa175","peanuts":73888},{"playerId":"0792d1ab-cf94-4160-a7ca-9ccdc84c48a2","peanuts":73888},{"playerId":"ccb923f3-a050-4c2e-b48f-fb3d3d8d031f","peanuts":71999},{"playerId":"88deffb6-9f94-42a5-9f8c-f528b37037b4","peanuts":71666},{"playerId":"05516e4f-980c-48eb-a5e1-07dbf6440468","peanuts":69444},{"playerId":"1719f12b-2750-48cc-955a-0eb453bec361","peanuts":69444},{"playerId":"4822b05d-3832-406d-bb27-05687880627b","peanuts":69444},{"playerId":"7f428ee5-863f-498d-ad63-840512e6130d","peanuts":65789},{"playerId":"bcacf464-be39-4981-8dd3-cbad4ccea695","peanuts":57333},{"playerId":"b5204124-6f90-46f6-bccc-b1ac11056cca","peanuts":56444},{"playerId":"d430b31c-79be-4d07-ab14-a35a9e3adefb","peanuts":55999},{"playerId":"26fca6cd-e865-4cc8-8ca1-7bd9c034fabe","peanuts":55888},{"playerId":"542a2f3a-9dac-44b8-a86f-3d585095a309","peanuts":54555},{"playerId":"22623721-b91d-40bb-9c8b-87e00a6c286a","peanuts":53555},{"playerId":"ded76037-e012-491a-94e0-2fd9f455f043","peanuts":52999},{"playerId":"847f0c8d-f3da-4f9e-ad38-3160c63ea419","peanuts":51777},{"playerId":"5cb75330-9e2c-4393-a5de-71ecd19ab9a1","peanuts":51777},{"playerId":"3fe1bce5-dbd1-40af-a116-5687f2a3281d","peanuts":50999},{"playerId":"5a18a67e-7470-4bcb-a789-ae0f42263144","peanuts":50999},{"playerId":"c90689ce-b0af-45bf-bd0a-09370c693686","peanuts":50999},{"playerId":"abe94094-bd47-43fc-8309-25a99738e8ee","peanuts":50999},{"playerId":"61e58265-b673-44f3-b263-f642a658b1ab","peanuts":50444},{"playerId":"ee4d7a67-d43d-4747-b007-8b58c4813a5e","peanuts":49555},{"playerId":"fa27ac29-bb67-4929-90d7-e0a14e2df73e","peanuts":49555}],"teams":[{"teamId":"88151292-6c12-4fb8-b2d6-3e64821293b3","peanuts":22194300},{"teamId":"d6a352fc-b675-40a0-864d-f4fd50aaeea0","peanuts":9233779},{"teamId":"9494152b-99f6-4adb-9573-f9e084bc813f","peanuts":9191228},{"teamId":"3a094991-4cbc-4786-b74c-688876d243f4","peanuts":8853185},{"teamId":"c19bb50b-9a22-4dd2-8200-bce639b1b239","peanuts":6492581},{"teamId":"54d0d0f2-16e0-42a0-9fff-79cfa7c4a157","peanuts":4849184},{"teamId":"4cd14d96-f817-41a3-af6c-2d3ed0dd20b7","peanuts":1131466},{"teamId":"1a51664e-efec-45fa-b0ba-06d04c344628","peanuts":673127},{"teamId":"2e22beba-8e36-42ba-a8bf-975683c52b5f","peanuts":472866},{"teamId":"55c9fee3-79c8-4467-8dfb-ff1e340aae8c","peanuts":441430},{"teamId":"cbd44c06-231a-4d1a-bb7d-4170b06e566a","peanuts":401880},{"teamId":"b6b5df8f-5602-4883-b47d-07e77ed9d5af","peanuts":223969},{"teamId":"1e04e5cc-80a6-41c0-af0d-7292817eed79","peanuts":180545},{"teamId":"258f6389-aac1-43d2-b30a-4b4dde90d5eb","peanuts":138394},{"teamId":"d0762a7e-004b-48a9-a832-a993982b305b","peanuts":127960}]}
  }
}
```


## apiGetUser
Gets the authenticated user. Must either be a string, or an object.

Defaults to Guest Season 20.

When it's a string, must be "guest s20".
When it's an object, it requires a property to define an endpoint.

### apiGetUser.type = "guest s20"
Returns a set of guest data, perfect for read-only viewing.

### apiGetUser.type = "static"
Returns a static set of data, parsed from `apiGetUser.data`.

Example:
```json
{
  "apiGetUser": {
    "type": "static",
    "data": {
      "appleId": "what's umpdog",
      "coins": 0,
      "email": "before@sibr.dev",
      "favoriteTeam": "b72f3061-f573-40d7-832a-5ad475bd7909",
      "id": "be457c4e-79e6-4016-94f5-76c6705741bb",
      "idol": "a1628d97-16ca-4a75-b8df-569bae02bef9",
      "lightMode": false,
      "packSize": 8,
      "snackOrder": [
        "Forbidden_Knowledge_Access",
        "Stadium_Access",
        "Wills_Access",
        "E",
        "E",
        "E",
        "E",
        "E"
      ],
      "snacks": {
        "Forbidden_Knowledge_Access": 1,
        "Stadium_Access": 1,
        "Wills_Access": 1
      },
      "spread": [],
      "trackers": {
        "BEGS": 3,
        "BETS": 10,
        "SNACKS_BOUGHT": 2,
        "SNACK_UPGRADES": 3,
        "VOTES_CAST": 1
      },
      "unlockedElection": true,
      "unlockedShop": true,
      "verified": true
    }
  }
}
```

## apiGetUserRewards
Gets the rewards for the authenticated user. Must either be a string, or an object.

Defaults to an empty set of data.

When it's a string, must be "empty".
When it's an object, it requires a property to define an endpoint.

### apiGetUserRewards.type = "empty"
Returns an empty set of data

### apiGetUserRewards.type = "static"
Returns a static set of data, parsed from `apiGetUserRewards.data`.

Example:
```json
{
  "apiGetUserRewards": {
    "type": "static",
    "data": {
      "coins": 0,
      "lightMode": false,
      "peanuts": 0,
      "toasts": []
    }
  }
}
```

## databaseFeedGlobal, databaseFeedGame, databaseFeedPlayer, databaseFeedTeam, databaseFeedStory
Returns a list of feed events from the corresponding {source}

Defaults to using [Upnuts](https://github.com/UnderMybrella/whats-upnut).

When it's a string, must be one of "upnuts", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseFeed{source}.type = "upnuts"
Returns data from Upnuts for the corresponding {source}, using only live Blaseball data for the instance's clock.

### databaseFeed{source}.type = "live"
Returns data from Blaseball for the corresponding {source}, time agnostic.

### databaseFeed{source}.type = "static"
Returns a static set of data, parsed from `databaseFeed{source}.data`.

Example:
```json
{
  "databaseFeedGlobal": {
    "type": "static",
    "data": [
      {
        "id": "aaf0a420-24b9-4de5-8aa6-fe91cfc6b8a3",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-07-04T18:02:48Z",
        "season": 21,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 13,
        "category": 4,
        "description": "The Breath Mints.",
        "nuts": 99999999999999,
        "metadata": {
          "_eventually_ingest_source": "blaseball.com",
          "_eventually_ingest_time": 1626558572,
          "being": 2,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      }
    ]
  }
}
```

## databaseAllDivisions
Gets **all** divisions for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseAllDivisions.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseAllDivisions.type = "live"
Returns data from Blaseball, time agnostic.

### databaseAllDivisions.type = "static"
Returns a static set of data, parsed from `databaseAllDivisions.data`.

Example:
```json
{
  "databaseAllDivisions": {
    "type": "static",
    "data": [{"id":"456089f0-f338-4620-a014-9540868789c9","name":"Mild High","teams":["36569151-a2fb-43c1-9df7-2df512424c82","b024e975-1c4a-4575-8936-a3754a08806a","b72f3061-f573-40d7-832a-5ad475bd7909","23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","105bc3ff-1320-4e37-8ef0-8d595cb95dd0","46358869-dce9-4a01-bfba-ac24fc56f57e"]},{"id":"5eb2271a-3e49-48dc-b002-9cb615288836","name":"Chaotic Good","teams":["bfd38797-8404-4b38-8b82-341da28b1f83","3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","979aee4a-6d80-4863-bf1c-ee1a78e06024","7966eb04-efcc-499b-8f03-d13916330531","36569151-a2fb-43c1-9df7-2df512424c82"]},{"id":"765a1e03-4101-4e8e-b611-389e71d13619","name":"Lawful Evil","teams":["8d87c468-699a-47a8-b40d-cfb73a5660ad","23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","f02aeae2-5e6a-4098-9842-02d2273f25c7","57ec08cc-0411-4643-b304-0e80dbc15ac7","747b8e4a-7e50-4638-a973-ea7950a3e739"]},{"id":"7fbad33c-59ab-4e80-ba63-347177edaa2e","name":"Chaotic Evil","teams":["eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","b63be8c2-576a-4d6e-8daf-814f8bcea96f","105bc3ff-1320-4e37-8ef0-8d595cb95dd0","a37f9158-7f82-46bc-908c-c9e2dda7c33b"]},{"id":"98c92da4-0ea7-43be-bd75-c6150e184326","name":"Wild Low","teams":["878c1bf6-0d21-4659-bfee-916c8314d69c","b63be8c2-576a-4d6e-8daf-814f8bcea96f","3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","f02aeae2-5e6a-4098-9842-02d2273f25c7","9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","bb4a9de5-c924-4923-a0cb-9d1445f1ee5d"]},{"id":"d4cc18de-a136-4271-84f1-32516be91a80","name":"Wild High","teams":["c73b705c-40ad-4633-a6ed-d357ee2e2bcf","a37f9158-7f82-46bc-908c-c9e2dda7c33b","ca3f1c8c-c025-4d8e-8eef-5be6accbeb16","747b8e4a-7e50-4638-a973-ea7950a3e739","57ec08cc-0411-4643-b304-0e80dbc15ac7","d9f89a8a-c563-493e-9d64-78e4f9a55d4a"]},{"id":"f711d960-dc28-4ae2-9249-e1f320fec7d7","name":"Lawful Good","teams":["b72f3061-f573-40d7-832a-5ad475bd7909","878c1bf6-0d21-4659-bfee-916c8314d69c","b024e975-1c4a-4575-8936-a3754a08806a","adc5b394-8f76-416d-9ce9-813706877b84","ca3f1c8c-c025-4d8e-8eef-5be6accbeb16"]},{"id":"fadc9684-45b3-47a6-b647-3be3f0735a84","name":"Mild Low","teams":["979aee4a-6d80-4863-bf1c-ee1a78e06024","eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","bfd38797-8404-4b38-8b82-341da28b1f83","7966eb04-efcc-499b-8f03-d13916330531","adc5b394-8f76-416d-9ce9-813706877b84","8d87c468-699a-47a8-b40d-cfb73a5660ad"]}]
  }
}
```

## databaseAllTeams
Gets **all** teams for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseAllTeams.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseAllTeams.type = "live"
Returns data from Blaseball, time agnostic.

### databaseAllTeams.type = "static"
Returns a static set of data, parsed from `databaseAllTeams.data`.

Example:
```json
{
  "databaseAllTeams": {
    "type": "static",
    "data": [{"id":"00245773-6f25-43b1-a863-42b4068888f0","card":-1,"emoji":"0x1F999","level":0,"state":{},"lineup":["c0b39229-1f79-4d8a-8444-d418017aa43a","a869e863-cfed-4995-ab14-2ad408df9316","737fa043-64e6-442c-a200-741809f8a73d","f3d757fc-9ac9-4a03-856d-2067ce3a02f7","fd8a5d0f-63c1-40e5-939a-62de6a230384","4d6f9123-04d1-4a43-969e-5ff9487cdab3","ec2bd1b4-0d36-45f5-a0bc-405c01cfe6d8","da0fb412-5b36-4156-974e-ccffccc7f103","7331e060-ed21-42b2-a024-3a27abae5919","b3682007-4275-4e1a-ac98-24ae728d43a1"],"slogan":"We are On Fire!","shadows":["ee17a92b-9058-4220-847d-4d132de57e8f","34f88a57-eeb8-435d-b844-a0654eb680ca","0f373afa-0f31-4a42-a230-a25bb19068c4","f2bdb219-2d10-481e-be7c-ef03eda6904d","17012262-1ea7-474c-b2fa-0723119e125b","0cf7110c-909c-42c9-945d-db22371e9d3b","3d83cd65-057f-4307-99e9-fd8d637d8e77","0bc0cc88-1be3-403b-999b-20f19f2319a4","293c2f48-94d3-4723-808b-52a00ddadc9f","b5d8f1c1-9e83-48e7-9d38-85d312fe895d"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"La Paz Llamas","gameAttr":[],"location":"La Paz","nickname":"Llamas","permAttr":[],"rotation":["b33e25bd-e616-48fb-b1b4-bf6ae59addcf","8f4ca0a5-4306-47b5-8fb4-00b325594184","493852a5-f812-4866-a951-3b2d4bd661e2","f3725fa6-c4a0-4eed-96bc-9beab7dd2cb7","8a1108a5-4612-4365-a63f-050c6d5bb8c5"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#4cae5c","shameRuns":0,"shorthand":"LPL","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":23,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":23,"seasonShamings":0,"secondaryColor":"#4cae5c","tournamentWins":0,"underchampionships":0},{"id":"105bc3ff-1320-4e37-8ef0-8d595cb95dd0","card":7,"emoji":"0x1F3B8","level":8,"state":{"permModSources":{}},"lineup":["e3c514ae-f813-470e-9c91-d5baf5ffcf16","44a22541-7704-403c-b637-3362a76943cb","d5c24c97-f3d3-4b3d-8c13-0debb61346c5","542af915-79c5-431c-a271-f7185e37c6ae","1301ee81-406e-43d9-b2bb-55ca6e0f7765","b88d313f-e546-407e-8bc6-94040499daa5","836e9395-3f83-4d42-ba11-a12c08ceb78b","8b53ce82-4b1a-48f0-999d-1774b3719202","1b7e9328-bda4-4961-a18f-53f43a5b4136","f3ddfd87-73a2-4681-96fe-829476c97886","c6a277c3-d2b5-4363-839b-950896a5ec5e"],"slogan":"Park It!","shadows":["061b209a-9cda-44e8-88ce-6a4a37251970","4ed61b18-c1f6-4d71-aea3-caac01470b5c","25581c43-f8da-4657-9e96-e704df0a8878","94844fad-9519-4c14-8ab3-d38606a7bb44","7e9a514a-7850-4ed0-93ab-f3a6e2f41c03","5f5764c7-c3a0-4dae-ad17-c6689f1e8c27","04651d05-44ef-40c1-abd3-4d0c14b6845d","d098ee1b-970f-4670-98a0-63dc43915c8d","ce58415f-4e62-47e2-a2c9-4d6a85961e1e","2d95d41c-ff54-4fa1-89a0-76b84dd8fe2b","f6b38e56-0d98-4e00-a96e-345aaac1e653","9c3273a0-2711-4958-b716-bfcf60857013","f968532a-bf06-478e-89e0-3856b7f4b124","dd0b48fe-2d49-4344-83ed-9f0770b370a8","d8742d68-8fce-4d52-9a49-f4e33bd2a6fc","f0594932-8ef7-4d70-9894-df4be64875d8","6bca5e98-4b0d-4127-83b1-0c4c527940be","b4bb9361-212e-4db1-b3d4-45b71bf80b6b","3696ec59-913b-46b8-aeea-169fab56891c","ac5e4ce2-de6f-408b-9910-a52bb1d1a978","ce0a156b-ba7b-4313-8fea-75807b4bc77f","df34d168-4d6a-403c-aca5-2b1ebe90444d","e9d764fa-8008-4f80-b60c-666f36b4c699","b39b5aae-8571-4c90-887a-6a00f2a2f6fd","63a31035-2e6d-4922-a3f9-fa6e659b54ad","82102ba8-c9b3-45f1-85a5-798ef769d9f2","0c83e3b6-360e-4b7d-85e3-d906633c9ca0","8b0d717f-ae42-4492-b2ed-106912e2b530","57b4827b-26b0-4384-a431-9f63f715bc5b","95007c81-012f-4068-84f3-e577348e61b5","5c6cce63-99b3-441d-90e0-0664e68057a6","fdfd36c7-e0c1-4fce-98f7-921c3d17eafe","418664aa-f28d-4eb6-92ba-ff9c7f45bd6e","da0bbbe6-d13c-40cc-9594-8c476975d93d","305921e8-3f4d-4c91-a280-d7bf1a449b08","960f041a-f795-4001-bd88-5ddcf58ee520","7853aa8c-e86d-4483-927d-c1d14ea3a34d","99e7de75-d2b8-4330-b897-a7334708aff9","4204c2d1-ca48-4af7-b827-e99907f12d61","378c07b0-5645-44b5-869f-497d144c7b35","7dcf6902-632f-48c5-936a-7cf88802b93a","62111c49-1521-4ca7-8678-cd45dacf0858","9032c905-5dec-4bb8-9140-47cfd7f16940"],"stadium":"cb94ac8b-003b-4d95-baf3-7c14b1c3fc29","deceased":false,"eDensity":3778.4151045957665,"fullName":"Seattle Garages","gameAttr":[],"location":"Seattle","nickname":"Garages","permAttr":[],"rotation":["6f9de777-e812-4c84-915c-ef283c9f0cde","f2468055-e880-40bf-8ac6-a0763d846eb2","68dd9d47-b9a8-4fd3-a89c-5c112eb1982e","defbc540-a36d-460b-afd8-07da2375ee63","81a0889a-4606-4f49-b419-866b57331383","1732e623-ffc2-40f0-87ba-fdcf97131f1f","f1cc4d7d-bef0-43dd-ba0a-17c7628aa775","0cb44026-ebf0-4870-b98e-e620b0814808"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#2b4075","shameRuns":0,"shorthand":"SEA","winStreak":-1,"imPosition":[0.8180523875102325,-0.6140709329779166],"teamSpirit":0,"totalShames":110,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":84,"seasonShamings":0,"secondaryColor":"#5a83ea","tournamentWins":0,"underchampionships":0},{"id":"1a51664e-efec-45fa-b0ba-06d04c344628","card":-1,"emoji":"0x1f9e0","level":0,"state":{},"lineup":["15b1b9d3-4921-4898-9f71-e33e8e11cae7","6a9b768f-8154-476c-b293-95d15617e1d0","8c00dbe3-47d6-4afb-8f29-92312a336548","7dc5a50e-85c0-4e8b-b2d5-151b720342c9","d7aa3455-072f-4133-bec5-55dd297d57ab","bb1eb01a-04d4-4e57-97c8-34cb80b250d9","5bf6faf9-392d-470f-95be-87276d0bc8e3","e2425d71-ae9f-49d5-bf89-a733bf3fe1e0","9b6c5cf3-af06-40ef-82de-ce4c88c452fe"],"slogan":"We Already Know.","shadows":["1da06cee-e1fd-4939-a8b0-159d6cc67faa","93784be0-1636-4ea6-82b3-6853a51a897f","4cd63a32-7fae-4e79-bd6b-5d8740ec3529","d69b57d8-9728-45d4-b888-07606c042d70","076750ca-42de-496a-808e-b9ddfc3e8788","c0210f64-e8a8-4a33-a40c-2b9210a2bfd4","b39f63a9-827c-4f23-8873-7bf8f47de4e0","4e328c64-85f0-4b6e-9ca3-146c1a886497","be15d63d-9c93-4e26-867a-680a59d437a3","ba13bd83-dc50-4f65-94f4-e3595244fc4c","549772ce-3a43-4936-8e50-ee162f7ebf7b"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Oregon Psychics","gameAttr":[],"location":"Oregon","nickname":"Psychics","permAttr":["PSYCHIC"],"rotation":["129a50ff-81a6-4860-9aed-116ae5db09ef","37e74dc8-3d6c-49f5-a4d0-952a9260e7d7","55886358-0dc0-4666-a483-eecb4e95bd7e","93f50e7a-5e0e-40e3-8191-01c39007586b"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#ad0155","shameRuns":0,"shorthand":"PSY","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":21,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":21,"seasonShamings":0,"secondaryColor":"#FA66AE","tournamentWins":0,"underchampionships":-1},{"id":"1e04e5cc-80a6-41c0-af0d-7292817eed79","card":-1,"emoji":"0x1F99E","level":0,"state":{},"lineup":["b038a790-0a49-4ad4-93eb-6a94327df4af","af140250-42d3-44c8-9ba5-f3852dffa73e","4152bfab-d88a-4985-8505-f8b25344e2cf","09ced838-2ac1-4638-834b-3389d1fd6ceb","6271225b-d40f-4e5a-bd3a-e73469c81b14","d430b31c-79be-4d07-ab14-a35a9e3adefb","09b7742e-814d-489b-ac2a-ee28eaa906b7","6a56aef4-4483-4c6c-9b93-b2ea8307f363","c6e9430e-775e-439c-b168-1567bea285ef"],"slogan":"Butter Up!","shadows":["eee1cf01-3074-4d87-a043-682f76e5270c","a6a482a5-feb3-45b7-9d61-61c95f0a7a9f","109454c9-5adc-4908-908a-b395be0e0cbf","3b96df08-88ec-4d2c-aeec-ef4855169691","85a794c3-76f9-4260-841b-669906f5e10e","f437b06f-c7ed-47e3-b1c1-ef6dffd0cb7e","c3103d64-ab14-463f-9514-b93afdabf1e3","4e0c5d69-9727-41bc-a39e-93a08b7cd285","debdad3a-d9f6-4d2a-bacf-d28c97732b97","38b05d71-b753-40b3-a32a-46cbb20002e7","799a254c-ae53-4959-8533-2fc3bede66fb"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Louisville Lobsters","gameAttr":[],"location":"Louisville","nickname":"Lobsters","permAttr":[],"rotation":["a8a982c3-f6ff-4a34-84e2-fff2adfef7ca","542a2f3a-9dac-44b8-a86f-3d585095a309","02aff1b1-3c55-42b3-b073-80f6ba5b5907","0b20b40e-43d3-4b7b-a338-3e9b3a14dc93","20c80028-f492-4f61-ac4c-2dca39f8dc8a"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#C2FA19","shameRuns":0,"shorthand":"LOU","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":19,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":19,"seasonShamings":0,"secondaryColor":"#D0FD48","tournamentWins":0,"underchampionships":0},{"id":"23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","card":8,"emoji":"0x1F967","level":4,"state":{"permModSources":{}},"lineup":["54bc7b23-49a9-4f1d-b60f-9c3cf9754b67","667cb445-c288-4e62-b603-27291c1e475d","ca26d8cc-8668-48c9-a061-28f6fdf5f44d","814bae61-071a-449b-981e-e7afc839d6d6","32810dca-825c-4dbc-8b65-0702794c424e","a7b0bef3-ee3c-42d4-9e6d-683cd9f5ed84","9f218ed1-d793-437d-a1b9-79f88f69154d"],"slogan":"Pie or Die.","shadows":["20395b48-279d-44ff-b5bf-7cf2624a2d30","d8bc482e-9309-4230-abcb-2c5a6412446d","cd5494b4-05d0-4b2e-8578-357f0923ff4c","c6e2e389-ed04-4626-a5ba-fe398fe89568","1750de38-8f5f-426a-9e23-2899a15a2031","190a0f31-d686-4ac4-a7f3-cfc87b72c145","dd6044ef-c635-4aed-97bd-d18068432b8f","4ec6951f-6822-4861-bc25-79826dd8c554","3d52ace6-a0a0-467e-80e0-ae86b289ad32","0672a4be-7e00-402c-b8d6-0b813f58ba96","7f379b72-f4f0-4d8f-b88b-63211cf50ba6","906a5728-5454-44a0-adfe-fd8be15b8d9b","90cc0211-cd04-4cac-bdac-646c792773fc","e495cadc-a645-439d-a556-e41de7493f18","198fd9c8-cb75-482d-873e-e6b91d42a446","b85161da-7f4c-42a8-b7f6-19789cf6861d","f245f6c6-4613-40f5-bc3b-85aa9ee3cf7e","1d3ca2ac-d430-4dd7-b322-176efecb8a09","f66fac56-fc42-4544-803a-0f84003d2eab","d73489d7-8edc-4f4e-89b4-01fd6f99b091","3ed9c552-a9a4-4cb3-8d7d-e1a2114f8515"],"stadium":"a109bd65-bfd3-43c7-9eb8-70dddc4d8a29","deceased":false,"eDensity":1580.2684045590422,"fullName":"Philly Pies","gameAttr":[],"location":"Philly","nickname":"Pies","permAttr":["AFFINITY_FOR_CROWS","AA"],"rotation":["d2a1e734-60d9-4989-b7d9-6eacda70486b","405dfadf-d435-4307-82f6-8eba2287e87a","ce99d7af-ba75-48a7-a5d8-d46f543829f2"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#399d8f","shameRuns":0,"shorthand":"PHIL","winStreak":1,"imPosition":[0.491569100732123,0.17987538211714002],"teamSpirit":0,"totalShames":116,"rotationSlot":0,"seasonShames":0,"championships":2,"totalShamings":79,"seasonShamings":0,"secondaryColor":"#58c3b4","tournamentWins":0,"underchampionships":0},{"id":"258f6389-aac1-43d2-b30a-4b4dde90d5eb","card":-1,"emoji":"0x1f417","level":0,"state":{},"lineup":["ba2d9b23-e076-40d2-8f89-e9e86dbbaaca","a88f1ac0-fefe-41fb-9e62-51c4310413f5","c4cc316a-4a3d-4a15-9a5b-fff014fbcabb","56bd6999-773f-4f5d-8c64-25658d30eb0c","70ccc231-6b10-4bea-aecd-4cfd80f09b95","160d01f4-b28c-46b4-8e28-9d38308f8b75","cb311121-d970-40b3-926f-027a0e0ea47c","ba274e6c-604e-42c3-80b3-6d338c8a6e35","f72647b4-49a0-4bd5-9f17-43c45cd9c1ce"],"slogan":"Ball Hogs","shadows":["1e07ea2e-5114-4c35-89d5-803c696a1fa3","ab789ef9-a8d1-48e5-92ee-1acbdf6cfdbc","aee14879-b924-450a-9eec-bfef97c575ab","64bcdd7c-164e-43ff-aa18-0e7899dc9174","d6278afa-9d11-4f46-b57e-86169bdcd0e0","584bd6ce-0e35-4b44-8674-c0bd3925fe64","4f1b32b7-44da-4374-9f86-c4eb32338135","ba5bc79a-afa3-47d9-a263-272cc2901d35","2fe49eb9-8709-421e-9040-4ab6afb0191d","fdda13f4-5070-45a7-8b29-0a3082de1072","a43ec79d-84ad-44bd-a7f4-c8b9fe658dee"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Kola Boar","gameAttr":[],"location":"Kola","nickname":"Boar","permAttr":[],"rotation":["1e74e188-ce50-4286-b867-838e6c566efb","f21d6c80-1e89-44e8-99ac-882e32c75cb6","28cac096-a8e8-4377-91e4-0252cf01c3a2","b14bae53-80ef-43cf-a117-3c9e7d223464","5927e930-b9d8-44d8-a91b-a57adab65d0d"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#3d82d1","shameRuns":0,"shorthand":"KOL","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":33,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":33,"seasonShamings":0,"secondaryColor":"#3d82d1","tournamentWins":0,"underchampionships":0},{"id":"280c587b-e8f6-4a7e-a8ce-fd2fa2fa3e70","card":0,"emoji":"0x1F4AB","level":0,"state":{},"lineup":[],"slogan":"Accretion","shadows":[],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Rising Stars","gameAttr":[],"location":"Rising","nickname":"Rising Stars","permAttr":[],"rotation":[],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#0c2357","shameRuns":0,"shorthand":"RisR","winStreak":1,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":2,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#41b9ff","tournamentWins":0,"underchampionships":0},{"id":"2e22beba-8e36-42ba-a8bf-975683c52b5f","card":-1,"emoji":"0x1F48E","level":0,"state":{},"lineup":["3b218380-d907-43c1-a207-c509bb36b320","22be4191-f541-4a2c-b9d3-5d6528638497","4284d9b6-9cab-44e5-a253-1c0fac2bb867","fc9c0ac4-f8ee-4147-a40e-d396ad41864d","5a18a67e-7470-4bcb-a789-ae0f42263144","4d2c6967-6d4e-4029-9361-58b0907aaf7e","0d8076a7-4910-4fec-bd48-f22aaa567457","08d48e58-2184-478d-8c03-fb79764f42fd","a6fa1c1a-08e8-4617-9a7d-1f91ecf652e3"],"slogan":"Winning is always in Vogue","shadows":["3fe1bce5-dbd1-40af-a116-5687f2a3281d","22380154-12f2-493a-94bb-9e65dd49bff7","f78ba0e2-0d21-474e-ab34-b2d475c85ad1","1d424e2a-d0f2-4c52-855b-d10064d6aca0","ffed12a4-2fc5-4b92-88ae-4359b474128d","21bc2e80-a7ef-4d76-8d6c-651e6a5dcd07","3b9e0412-dc73-4a4c-abbd-c58496994161","f6c92735-98c5-4897-a9b1-3b2fb6ff4078","5a694dd9-2c47-464d-ae4a-6e0fe8614458","e8179145-6a32-4a16-9e2a-041fe9ab532f","bad4e663-b075-4003-83b8-e0ec799f37fd"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Carolina Queens","gameAttr":[],"location":"Carolina","nickname":"Queens","permAttr":[],"rotation":["61e58265-b673-44f3-b263-f642a658b1ab","a5f1679d-870e-4da6-8f29-445d67f185ba","76a6fad8-10ff-4199-acf0-99e95edbd24b","b0421cb2-e653-4581-9b83-04c8381e58c0"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#552583","shameRuns":0,"shorthand":"CAR","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":22,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":22,"seasonShamings":0,"secondaryColor":"#552583","tournamentWins":0,"underchampionships":0},{"id":"3543229a-668c-4ac9-b64a-588422481f12","card":-1,"emoji":"0x1f42c","level":0,"state":{},"lineup":["bbbc1567-af13-4457-b61a-47bcb76cbd3f","80d59733-6156-41ce-9bae-f197b947f5e6","22dfe1e0-c809-4bcb-bf4b-72a79d11e063","8c9c7827-9b20-49f2-8e64-cb1ff09351b0","94d0c938-637a-4169-bfc8-e337e27f1522","0148c1b0-3b25-4ae1-a7ce-6b1c4f289747","1c49ea43-7c93-41ff-9bcb-59a2928527bd","4a73b0bd-1d10-4b12-adea-5ae07c57223b","cfbd14e3-0948-4c46-806c-8e4bca248e18"],"slogan":"EEeeeEEEEEeeeEEEEe!","shadows":["0b2480b4-b5f9-44a9-b843-807c5a9b0c6b","583909ca-a60f-47eb-b5fc-d0b6c25dde0c","661f3359-7e06-456d-8f2a-d06d4363a086","8f487944-c132-4f7d-a3aa-96d20449f345","03acbf6c-81aa-44c5-a494-ef3335377cba","e56fe417-7385-4eee-8db8-b9eb71410621","73f9a23d-0f78-465d-8a8c-31462b5b6bcf","f2ec4c2b-ec91-4bc1-9a37-fd612e7e7d1d","4699d47d-73b0-45ad-8c35-c4f715ed236e","9777ac8d-bedc-4605-9379-06660e0b272d","6f74c31b-56a0-4174-849b-6d6e297f5a03"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Wyoming Dolphins","gameAttr":[],"location":"Wyoming","nickname":"Dolphins","permAttr":[],"rotation":["0e43b265-aa19-43ff-8e77-f3d6b3bebad0","0c6c10c5-0515-4f79-b2c8-03b98373da45","a2471aa1-03cb-4c58-babf-b1f95d2098b6","caa6f958-4f3e-4bd8-b0e3-4e624d9ccc5e","293deb94-e4c4-4ea8-b954-8233ddc05a87"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#FCC56B","shameRuns":0,"shorthand":"FINS","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":138,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":138,"seasonShamings":0,"secondaryColor":"#FCCE85","tournamentWins":0,"underchampionships":1},{"id":"36569151-a2fb-43c1-9df7-2df512424c82","card":5,"emoji":"0x1F4F1","level":0,"state":{"permModSources":{}},"lineup":["4f615ee3-4615-4033-972f-79200f9db6e3","ec68845f-3b26-412f-8446-4fef34e09c77","4b3e8e9b-6de1-4840-8751-b1fb45dc5605","5dbf11c0-994a-4482-bd1e-99379148ee45","81d7d022-19d6-427d-aafc-031fcb79b29e","16aff709-e855-47c8-8818-b9ba66e90fe8","6c346d8b-d186-4228-9adb-ae919d7131dd","4ffd2e50-bb5b-45d0-b7c4-e24d41b2ff5d","f38c5d80-093f-46eb-99d6-942aa45cd921"],"slogan":"Youth Will Save Us","shadows":["1e7b02b7-6981-427a-b249-8e9bd35f3882","6a869b40-be99-4520-89e5-d382b07e4a3c","afd236cf-d8cb-42cc-b862-c17624cf1784","662f34f0-bd74-49a7-8506-50d4dd333c4c","c54fba12-2fb5-4091-b1d7-1449693b35d6","98220753-0a70-4a4e-8058-0a7bcb6f7d5d","138fccc3-e66f-4b07-8327-d4b6f372f654","8b5ddd3d-cca6-4e01-ac7f-65dc56831933","ab9b2592-a64a-4913-bf6c-3ae5bd5d26a5","29bf512a-cd8c-4ceb-b25a-d96300c184bb","8d81b190-d3b8-4cd9-bcec-0e59fdd7f2bc","94d772c7-0254-4f08-814c-f6fc58fcfb9b","c4dec95e-78a1-4840-b209-b3b597181534","5c60f834-a133-4dc6-9c07-392fb37b3e6a","b7cdb93b-6f9d-468a-ae00-54cbc324ee84","c4951cae-0b47-468b-a3ac-390cc8e9fd05","5d063a91-31b3-4688-97a7-e34a7181da30","cc113432-5f9b-46f8-9745-09f999d51801","766dfd1e-11c3-42b6-a167-9b2d568b5dc0","947ea345-6d0c-44b8-9126-90b9d247fd0f","21ea41b6-c122-46d9-ac09-3e23caad17f0"],"stadium":"ba794e99-4d6b-4e12-9450-6522745190f8","deceased":false,"eDensity":-275.73694887328975,"fullName":"New York Millennials","gameAttr":[],"location":"New York","nickname":"Millennials","permAttr":["BIRD_SEED"],"rotation":["2c4b2a6d-9961-4e40-882c-a338f4e72117","c0c85be4-ff26-470f-8135-af771fd21e51","7a75d626-d4fd-474f-a862-473138d8c376","aae38811-122c-43dd-b59c-d0e203154dbe","ae4acebd-edb5-4d20-bf69-f2d5151312ff"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#ffd4d8","shameRuns":0,"shorthand":"NYMI","winStreak":-2,"imPosition":[-0.4887398589768893,0.8503850198336724],"teamSpirit":0,"totalShames":97,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":98,"seasonShamings":0,"secondaryColor":"#ffd4d8","tournamentWins":0,"underchampionships":0},{"id":"3a094991-4cbc-4786-b74c-688876d243f4","card":-1,"emoji":"0x1F43F","level":0,"state":{},"lineup":["38f3ba48-47aa-4116-be5f-91fbcebd82f7","993a0abd-52b8-43b1-8a74-e0360dca1ea7","e5a8901f-177e-486d-86b8-d3aefcd5a68d","b93bdf99-d9b2-4717-9798-63bd22b0d7da","1b2653a7-602a-4688-83ab-f68e23c79528","37b7c464-ae5d-466d-bd09-e0af60e687f0","76d7c62f-5bcd-4285-9e45-01fd3f54a1c0","a49e4c5c-8651-4c34-b737-d0fc362cf920"],"slogan":"run run run run run run","shadows":["bdde3fcb-1142-4c71-a44b-d912cecc6b00","1f031a8a-1a8b-4317-833d-0a774d475e17","674b6589-9f74-4d93-92db-0eb01d1f74ca","77beccc1-2130-4b55-8248-246ded5835a7","e9ea53b0-0b82-4f8a-89c4-65d6f95662cf","6f924ae0-1613-488f-add0-bce1eb60845a","4fe9f171-37f4-4c89-9ad4-d6948afa7179","26732fde-ebfb-4d91-a686-e5ce94883dc0","b020f3d0-126c-4879-a530-9a67402f06a3","68416f98-6dd8-43e6-afba-8be71136afcd","79df0468-f66b-4e25-857e-a6aaff553de6"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Maryland Squirrels","gameAttr":[],"location":"Maryland","nickname":"Squirrels","permAttr":[],"rotation":["c6b930e3-1e4a-441e-9f31-b41bb9d3fdf5","1c0d9309-53be-4e2b-8f46-d846cc4af481","b628e10e-ad1f-4d6f-879d-2bbfa5213367","2ab5811d-a5dd-4059-ac4c-5cbc39fc537c","bfa9e92a-0949-4629-86e6-cebf12dc2ad6"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#91cacf","shameRuns":0,"shorthand":"RUN","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":46,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":46,"seasonShamings":0,"secondaryColor":"#91cacf","tournamentWins":0,"underchampionships":0},{"id":"3b0a289b-aebd-493c-bc11-96793e7216d5","card":-1,"emoji":"0x1F3A8","level":null,"state":{},"lineup":["1d7af93e-7910-45c3-9799-4166a27a6d0b","8a4dc2bb-ac5b-4774-86bf-c7f8d3000a4a","520b2a14-ef60-4ee8-9f39-c7be12971ef2","4a1927e7-88aa-4b2c-a182-7ec247dc2647","05415c0d-db3b-455d-9d06-684a39506cf5","e492fe7e-1570-4d5e-92a2-7cfb2cfee5a6","7f7042de-7fc1-45f9-b7c4-9169d1454653","50aad46d-be61-4236-bb0c-6c6198e00be1","f63ee68f-8021-4dd0-b3d5-b3d587ae3213","4957826f-0c44-467d-b71f-21185450f458"],"slogan":"Paint the Town Dead!","shadows":["3c53ff1c-6437-4965-a4a2-489bcb91d3f4","01bcbeeb-0f63-43b4-8cfa-ec02b42a1a5e","478d91ef-1a6d-4bb3-9965-87cdcc4ad8db","e2ad81ff-fff2-40bb-9043-95bdaf34ebc2","caa9bd86-244a-4aba-a237-6a9528f09570","6b2722a6-b371-4a7e-8adf-728da612a40c","b3c8c0a0-20a0-4c5d-a391-34b99038693c","43e4de02-cc21-4413-bb88-1c1128eec174","a083246b-35f4-4dad-8b07-8b28047e0823","6c1c0539-4168-4977-bf17-257bcbfec92b","87983006-c9c5-459f-8e7c-e7edcbd902dc","f359f979-f658-4427-82a3-7185377c85c3","bde7bdc5-e0a8-4990-a6b6-c4909c3ebbc7","fe1f826f-7346-425c-9d3c-7ed5a7eae1b7","30b7053a-7f5f-4e3b-9600-7b3da8ba7dc0","aba07c28-e410-4710-b426-b3eba396930f"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Pandemonium Artists","gameAttr":[],"location":"Pandemonium","nickname":"Artists","permAttr":["COFFEE_SHADOWS"],"rotation":["de3836a4-fdba-46cc-ab53-49fcaacc39c0","4029ea75-da91-4672-9fa5-ee32ec7fa713","30dc0652-2608-49ba-8929-36494fc2bd29","48a10ad6-6759-4661-80bf-9f9fe393162c","7d45e942-ac28-41b6-8d89-5e4a44591c11","01d3ef84-8ec2-4116-9a7b-5f954de2ec97"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#000000","shameRuns":0,"shorthand":"PA","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":4,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ffffff","tournamentWins":0,"underchampionships":0},{"id":"3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","card":2,"emoji":"0x1F339","level":4,"state":{"permModSources":{}},"lineup":["d0d7b8fe-bad8-481f-978e-cb659304ed49","a647388d-fc59-4c1b-90d3-8c1826e07775","855775c1-266f-40f6-b07b-3a67ccdf8551","04931546-1b4a-469f-b391-7ed67afe824c","e4034192-4dc6-4901-bb30-07fe3cf77b5e","28964497-0efe-420c-9c1d-8574f224a4e9","3e008f60-6842-42e7-b125-b88c7e5c1a95","81b25b16-3370-4eb0-9d1b-6d630194c680","c09e64b6-8248-407e-b3af-1931b880dbee","4e6ad1a1-7c71-49de-8bd5-c286712faf9e","718dea1a-d9a8-4c2b-933a-f0667b5250e6","be18d363-752d-4e4a-b06b-1a7e4641400b","51c5473a-7545-4a9a-920d-d9b718d0e8d1"],"slogan":"Let's Grow!","shadows":["73a934de-3db1-4553-97f2-9e7b57744b34","d9a072f5-1cbb-45ce-87fb-b138e4d8f769","de67b585-9bf4-4e49-b410-101483ca2fbc","4dc3ea1a-f008-44de-9fbe-4811552ef662","710541f4-bb89-4134-8973-958c82b29a41","dfd5ccbb-90ed-4bfe-83e0-dae9cc763f10","6caa8180-e178-4f8b-865c-f1af47119bc2","a02b09f5-6df3-4732-b1c4-0d65246546a7","51985516-5033-4ab8-a185-7bda07829bdb","98f26a25-905f-4850-8960-b741b0c583a4","4b73367f-b2bb-4df6-b2eb-2a0dd373eead","a938f586-f5c1-4a35-9e7f-8eaab6de67a6","8acc97fa-4bf1-49f6-bb93-fb52513a015b","2cadc28c-88a5-4e25-a6eb-cdab60dd446d","8cd06abf-be10-4a35-a3ab-1a408a329147","828c5080-4543-4bf2-9d84-436d58c7fd66","a10b58c7-d3f5-4757-94f2-62d8862b8398","c771abab-f468-46e9-bac5-43db4c5b410f","d359c61b-e142-4ead-9698-8edc8b2211f7","a98917bc-e9df-4b0e-bbde-caa6168aa3d7","b348c037-eefc-4b81-8edd-dfa96188a97e","87f5c53a-46fe-41a6-b3f7-7d3dc90ac1bf","df94a335-1677-4200-a23f-d794689a5732","fa58f623-3578-4ffc-8f04-545505e20445"],"stadium":"d37abba6-9fba-463a-8f4f-7cf49de3c7b6","deceased":false,"eDensity":1616.254340650461,"fullName":"Boston Flowers","gameAttr":[],"location":"Boston","nickname":"Flowers","permAttr":["GROWTH"],"rotation":["f3c07eaf-3d6c-4cc3-9e54-cbecc9c08286","2da49de2-34e5-49d0-b752-af2a2ee061be","b5204124-6f90-46f6-bccc-b1ac11056cca","e3e1d190-2b94-40c0-8e88-baa3fd198d0f","ff5a37d9-a6dd-49aa-b6fb-b935fd670820"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#f7d1ff","shameRuns":0,"shorthand":"BOF","winStreak":1,"imPosition":[0.7818050211899579,0.16690656713235213],"teamSpirit":0,"totalShames":100,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":102,"seasonShamings":0,"secondaryColor":"#f7d1ff","tournamentWins":0,"underchampionships":0},{"id":"40b9ec2a-cb43-4dbb-b836-5accb62e7c20","bench":[],"emoji":"0x1F95C","lineup":["9820f2c5-f9da-4a07-b610-c2dd7bee2ef6","5ca7e854-dc00-4955-9235-d7fcd732ddcf","8903a74f-f322-41d2-bd75-dbf7563c4abb","a1ed3396-114a-40bc-9ff0-54d7e1ad1718","f741dc01-2bae-4459-bfc0-f97536193eea","ea44bd36-65b4-4f3b-ac71-78d87a540b48","de21c97e-f575-43b7-8be7-ecc5d8c4eaff","5ff66eae-7111-4e3b-a9b8-a9579165b0a5","86d4e22b-f107-4bcf-9625-32d387fcb521","083d09d4-7ed3-4100-b021-8fbe30dd43e8","667cb445-c288-4e62-b603-27291c1e475d"],"slogan":"TREMBLE BEFORE MY PODS","bullpen":[],"fullName":"THE SHELLED ONE'S PODS","gameAttr":[],"location":"THE SHELLED ONE'S","nickname":"PODS","permAttr":["PEANUT_RAIN","CRUNCHY","GOD","EXTRA_STRIKE","CURSE_OF_CROWS","SUBJECTION","DESTRUCTION"],"rotation":["04e14d7b-5021-4250-a3cd-932ba8e0a889"],"seasAttr":[],"weekAttr":[],"mainColor":"#ff0000","shameRuns":2,"shorthand":"THEP","teamSpirit":29918000,"totalShames":1,"rotationSlot":3,"seasonShames":1,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ff0000"},{"id":"46358869-dce9-4a01-bfba-ac24fc56f57e","card":-1,"emoji":"0x1F6E0","level":1,"state":{"permModSources":{}},"lineup":["2f85d731-81ed-4a07-9e01-e93f1786a366","a1fba555-a48c-44f3-afb6-522ea256947c","9ac2e7c5-5a34-4738-98d8-9f917bc6d119","c83b82e8-b8d2-4cb7-9982-97593283acc2","b643a520-af38-42e3-8f7b-f660e52facc9","c0998a08-de15-4187-b903-2e096ffa08e5"],"slogan":"We Can Fix This","shadows":["7e910e86-84b4-43c3-a68e-67b6612ddbee","84a17f48-0393-4166-864a-f7cd27557701","9e75ff7c-fe9b-428b-a1b6-f9e6a1748c86","f7de0227-6739-48e2-bbd6-9dc7b247ab9a","18215d90-c5d6-4ebe-be56-980f58aeef50","6f9f50e6-81bf-4371-9a27-de3c85bd5a82","9311b727-de74-4f25-912d-e08a79f061b0","7541fc4b-7e6b-446a-b6a3-755de46a0018","efda41c1-eb07-45b3-8ad5-424c63115115","5991ccb3-7eed-46d9-9d7c-69dec9b56d4b","eac00ec9-3376-423c-8684-93dd2cbd6ed4","30fa4bf1-8f3b-45dc-9aa3-6511e998040a","9c813008-b2a9-4257-80e7-a709c67189fe","096bb9b5-5ea8-4da1-a113-57a5fc9e9b67","b0c20916-7097-47e6-8226-cbf67da176e0","b629cb7a-414b-4695-a115-7528ab700346","13676ab5-afb3-4650-af23-bcde1037d25b","4c86a500-d538-4b83-b321-d762533d7660","51196a91-509a-4208-b66e-ea676ab6cd7e","d796d287-77ef-49f0-89ef-87bcdeb280ee","5b3f0a43-45e7-44e7-9496-512c24c040f0","16a668a3-f184-4d84-885f-680b4a64380b"],"stadium":"031670ae-97a5-4215-b1a1-98e9f1de7c50","deceased":false,"eDensity":199.45491264511963,"fullName":"Core Mechanics","gameAttr":[],"location":"Core","nickname":"Mechanics","permAttr":["MAINTENANCE_MODE","STALEPOPCORN_PAYOUTS","FREE_WILL"],"rotation":["888fce7a-0d21-478a-9b45-4662193fe9aa","5bc7e5d4-39be-4d64-a242-abb39aca6f42","3ebb5361-3895-4a50-801e-e7a0ee61750c","266abcc0-8abc-4570-b32e-e5668e5d38f3"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#d13100","shameRuns":0,"shorthand":"CORE","winStreak":1,"imPosition":[-0.2373780405826838,0.6786888848125352],"teamSpirit":0,"totalShames":56,"rotationSlot":0,"seasonShames":0,"championships":5,"totalShamings":41,"seasonShamings":0,"secondaryColor":"#ff5b29","tournamentWins":0,"underchampionships":0},{"id":"49181b72-7f1c-4f1c-929f-928d763ad7fb","card":-1,"emoji":"0x1F3C6","level":null,"state":{},"lineup":["c675fcdf-6117-49a6-ac32-99a89a3a88aa","e16c3f28-eecd-4571-be1a-606bbac36b2b","f3ddfd87-73a2-4681-96fe-829476c97886","3d3be7b8-1cbf-450d-8503-fce0daf46cbf","be18d363-752d-4e4a-b06b-1a7e4641400b","2e86de11-a2dd-4b28-b5fe-f4d0c38cd20b","5ff66eae-7111-4e3b-a9b8-a9579165b0a5","e2f39815-5291-4dcf-ba19-97dcf0c015e9","4f69e8c2-b2a1-4e98-996a-ccf35ac844c5"],"slogan":"Teamwork makes the steam work","shadows":["a8a5cf36-d1a9-47d1-8d22-4a665933a7cc","f2468055-e880-40bf-8ac6-a0763d846eb2","ae81e172-801a-4236-929a-b990fc7190ce","316abea7-9890-4fb8-aaea-86b35e24d9be","9f85676a-7411-444a-8ae2-c7f8f73c285c","bd24e18b-800d-4f15-878d-e334fb4803c4","30218684-7fa1-41a5-a3b3-5d9cd97dd36b","cfb42325-592a-4119-9b40-4cdc19959b09","21d52455-6c2c-4ee4-8673-ab46b4b926b4"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Atlético Latte","gameAttr":[],"location":"Latte","nickname":"Atlético","permAttr":[],"rotation":["80dff591-2393-448a-8d88-122bd424fa4c","1513aab6-142c-48c6-b43e-fbda65fd64e8","4ffd2e50-bb5b-45d0-b7c4-e24d41b2ff5d","bf122660-df52-4fc4-9e70-ee185423ff93","dd6044ef-c635-4aed-97bd-d18068432b8f"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#094f12","shameRuns":0,"shorthand":"AL","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":10,"seasonShames":0,"championships":0,"totalShamings":1,"seasonShamings":1,"secondaryColor":"#36ad45","tournamentWins":0,"underchampionships":0},{"id":"4cd14d96-f817-41a3-af6c-2d3ed0dd20b7","card":-1,"emoji":"0x1F9C2","level":0,"state":{},"lineup":["7f428ee5-863f-498d-ad63-840512e6130d","5a7e4947-ac24-4b46-a229-ea8dae7089d6","8beda710-20ed-4de2-afde-5fd0f6ec580a","7d5e87c7-57b6-4481-b141-e8fd4687673a","fb997c06-23f7-4893-87dd-712a5afba567","d3cdafd0-4026-4af8-b744-6982a0bfe3fa","b974731e-cf85-4add-b96f-13bb54bf4114","825be53e-c5eb-4125-bee4-0c898549edf5"],"slogan":"Stay Salty","shadows":["26711680-b5fa-4780-9440-01249e49f6d0","82dcd0de-3a41-46b7-979b-98e9b75b3f7a","0b8c40ec-326d-4041-863a-6090d6e5bc5a","598b3771-c18c-4ea2-bd6b-0e163dbbeaf4","16334652-e342-4805-bc8e-f149dbe6b96a","acfd3082-2fe2-46d2-a00f-231c33f2bdcf","72722d3b-4c93-44f2-8cad-0b72779070ab","f7696a14-6dca-4984-9af6-f5d73ff475e4","ab5d4fd7-fd08-4571-9a5e-2b62e7f4be7b","fbdbd9ac-37ac-4cd9-9e57-ae619cef8e49","f0c4f556-acfe-4792-959e-cd5274d5c8f9"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"San Diego Saltines","gameAttr":[],"location":"San Diego","nickname":"Saltines","permAttr":[],"rotation":["f7579733-88b8-4cee-9284-3065451418c6","a97d73ff-e69f-473b-bd71-1e42f3cf7ff8","ba456a81-2dd5-4e8c-9712-8b9ac389d430","27711668-e460-4094-a87c-1f7ef0ff9dff","6b2cf5b5-88bf-437e-b81d-51e70d942230"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#9B8B5A","shameRuns":0,"shorthand":"SALT","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":2,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":2,"seasonShamings":0,"secondaryColor":"#BDA96E","tournamentWins":0,"underchampionships":0},{"id":"4d921519-410b-41e2-882e-9726a4e54a6a","card":-1,"emoji":"0x2744","level":null,"state":{},"lineup":["190a0f31-d686-4ac4-a7f3-cfc87b72c145","44c92d97-bb39-469d-a13b-f2dd9ae644d1","f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1","1732e623-ffc2-40f0-87ba-fdcf97131f1f","1f159bab-923a-4811-b6fa-02bfde50925a","1c73f91e-0562-480d-9543-2aab1d5e5acd","c31d874c-1b4d-40f2-a1b3-42542e934047","80de2b05-e0d4-4d33-9297-9951b2b5c950","18af933a-4afa-4cba-bda5-45160f3af99b"],"slogan":"Blaseball Go Brrrrrr","shadows":["46721a07-7cd2-4839-982e-7046df6e8b66","e4e4c17d-8128-4704-9e04-f244d4573c4d","5a26fc61-d75d-4c01-9ce2-1880ffb5550f","b7267aba-6114-4d53-a519-bf6c99f4e3a9","b39b5aae-8571-4c90-887a-6a00f2a2f6fd","042962c8-4d8b-44a6-b854-6ccef3d82716","8cd06abf-be10-4a35-a3ab-1a408a329147","bbf9543f-f100-445a-a467-81d7aab12236"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Cold Brew Crew","gameAttr":[],"location":"Cold Brew","nickname":"Cold Brew","permAttr":[],"rotation":["f2a27a7e-bf04-4d31-86f5-16bfa3addbe7","e111a46d-5ada-4311-ac4f-175cca3357da","3c331c87-1634-46c4-87ce-e4b9c59e2969","aa6c2662-75f8-4506-aa06-9a0993313216","68dd9d47-b9a8-4fd3-a89c-5c112eb1982e"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#2a5e6c","shameRuns":0.5,"shorthand":"CBC","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":1,"rotationSlot":5,"seasonShames":1,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#92c7d6","tournamentWins":0,"underchampionships":0},{"id":"4e5d0063-73b4-440a-b2d1-214a7345cf16","card":-1,"emoji":"0x1F4A7","level":null,"state":{},"lineup":["611d18e0-b972-4cdd-afc2-793c56bfe5a9","efafe75e-2f00-4418-914c-9b6675d39264","8ecea7e0-b1fb-4b74-8c8c-3271cb54f659","4ca52626-58cd-449d-88bb-f6d631588640","b1b141fc-e867-40d1-842a-cea30a97ca4f","58c9e294-bd49-457c-883f-fb3162fc668e","a1ed3396-114a-40bc-9ff0-54d7e1ad1718","63df8701-1871-4987-87d7-b55d4f1df2e9","b8ab86c6-9054-4832-9b96-508dbd4eb624"],"slogan":"Water Down!","shadows":["8cf78b49-d0ca-4703-88e8-4bcad26c44b1","f38c5d80-093f-46eb-99d6-942aa45cd921","c0177f76-67fc-4316-b650-894159dede45","285ce77d-e5cd-4daa-9784-801347140d48","c182f33c-aea5-48a2-97ed-dc74fa29b3c0","ce0e57a7-89f5-41ea-80f9-6e649dd54089","2e6d4fa9-f930-47bd-971a-dd54a3cf7db1","733d80f1-2485-40f7-828b-fd7cd8243a01","f4a5d734-0ade-4410-abb6-c0cd5a7a1c26","413b3ddb-d933-4567-a60e-6d157480239d"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Americano Water Works","gameAttr":[],"location":"Americano","nickname":"Americano","permAttr":[],"rotation":["c0732e36-3731-4f1a-abdc-daa9563b6506","b6aa8ce8-2587-4627-83c1-2a48d44afaee","82d1b7b4-ce00-4536-8631-a025f05150ce","2da49de2-34e5-49d0-b752-af2a2ee061be","089af518-e27c-4256-adc8-62e3f4b30f43"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#005d9c","shameRuns":0,"shorthand":"AWW","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":5,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#8dd4f0","tournamentWins":0,"underchampionships":0},{"id":"53d473fb-ffee-4fd3-aa1c-671228adc592","card":-1,"emoji":"0x1f346","level":0,"state":{},"lineup":["7df00766-95a1-4a95-8018-608d69d58ac3","707c4a70-1184-4081-882b-6f70bbc961f6","7529fdc0-aced-47a5-a9df-a17092c9c4ef","2c029ec7-dfeb-4d4a-8cfd-6f78ab21c51a","83634a49-7422-46fe-9afb-6346cca779e1","19b78612-65b2-4344-9f2d-c6cb232ef146","4c8b4d54-a129-46bd-a1d5-46d3dd1757fc","e25ba8a8-c084-4141-9022-508cc30f7f83","71b1f994-3767-4c58-aace-b5ba1210d911"],"slogan":"Veg Out!","shadows":["c6d91e41-1509-4e5d-84cc-1d8af869d492","5727610e-41f2-472c-80e2-56aa47993755","56419d84-76be-4489-85f0-5635c010330e","f1e81503-545c-40b1-ba8c-41ce5a673a4b","42ac4519-eb2a-41bf-b85a-c75129776257","81b9a3d2-3344-4b1e-8a08-7994aa640467","647e574b-5687-4d5b-9e5d-18fcbcdef5aa","b215ddc2-196c-4f45-8302-4104e516e530","c5d1e6f2-f8f7-48fe-bc3d-3aa42b44788d","63d5845d-f077-44ca-91fd-e33ac85af560","ed78d22b-d4a3-4270-b2b0-c93f52f445c3"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"New Hampshire Eggplants","gameAttr":[],"location":"New Hampshire","nickname":"Eggplants","permAttr":["O_NO","UNFLAMED","NIGHTSHADE"],"rotation":["2246bedf-b53d-49f7-ac9e-e52abc1f38ea","0a1c3949-8927-4eae-a189-f4e073955ba4","bda2eefc-17fa-4c64-abb6-993320e6a22a","7118ca3f-413d-4336-90d2-af8fbfc1e8f2"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#b0fdb8","shameRuns":0,"shorthand":"AUB","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":66,"rotationSlot":0,"seasonShames":0,"championships":3,"totalShamings":66,"seasonShamings":0,"secondaryColor":"#05d41e","tournamentWins":0,"underchampionships":0},{"id":"54d0d0f2-16e0-42a0-9fff-79cfa7c4a157","card":-1,"emoji":"☄️","level":0,"state":{},"lineup":["25f72ce9-450c-4457-b62a-3fe9a57e7fc8","8eee96eb-58c9-4ecb-91c8-c461a72f9454","26fca6cd-e865-4cc8-8ca1-7bd9c034fabe","86fd237d-29ca-4ceb-8c2e-dac12b912698","c90689ce-b0af-45bf-bd0a-09370c693686","90d853ef-7865-48f3-96c2-d7ceefec2ee5","e22a3609-e94d-4e7c-a9eb-2064e3261116","8f563500-0b23-4491-a0e9-bb95298e11aa","44689655-1b85-4758-aa58-fab2b98144a7"],"slogan":"So Cold, We're Hot!","shadows":["f462061d-d8cb-4505-9a12-b083f790c442","2da5741e-1464-4701-9762-671cc4d2c90a","c7f1d523-5866-40cb-9878-7ee0b8ffbbaf","464ad5b1-6e9d-4827-a5a6-7955b11d30cf","8d20801f-73e1-4a36-beca-b5fd961f7c1d","71ac04ae-8210-44e2-8cad-cda68b489d13","abe94094-bd47-43fc-8309-25a99738e8ee","2db72ae0-d690-4960-bc40-b6de96857ce3","c878047a-d9d4-44cc-a0a7-2dcd460a9b35","b66714f7-b20e-4fad-931d-5e21dc4a3dc6","5a343e8f-4196-4164-a43a-7c144fbf8402"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Antarctic Fireballs","gameAttr":[],"location":"Antarctic","nickname":"Fireballs","permAttr":[],"rotation":["677b897c-92a5-4f65-ae77-99f0701c76cc","567b1c3c-50ba-41b4-9029-5c319ad32a6e","bb04bda7-5e27-4d3c-a3ea-d503461dca71","22623721-b91d-40bb-9c8b-87e00a6c286a"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#4a7efc","shameRuns":0,"shorthand":"ANT","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":16,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":16,"seasonShamings":0,"secondaryColor":"#7199fc","tournamentWins":0,"underchampionships":0},{"id":"55c9fee3-79c8-4467-8dfb-ff1e340aae8c","card":-1,"emoji":"0x1f404","level":0,"state":{},"lineup":["e461f992-1008-49a4-aeb5-0dfe2ce0bd6f","9453e2e6-600b-48e3-98d9-47a8880277e7","87614491-a213-411c-ad75-e02aac025366","fa13dfab-1e84-437f-a5fe-adc54c0a0968","92af85c2-78e2-460f-b1a7-462f95436de5","6ed20de2-8423-4d65-adf6-915121553008","f828e413-f0a7-4ebd-a92c-37eda14be467","003b6d29-1797-4762-a4ba-5ff574aeaa43","e846cba2-31b3-4e30-aaee-de490c35ac84"],"slogan":"Udder Domination.","shadows":["ba76c6d8-94f0-4582-95d3-c16f49c16356","ec029d42-3fcb-425a-b719-aad45423e1d4","a30a1f01-3f6b-4dd6-883a-d5eda543a468","71da0154-51a5-49b6-acc7-b44898fbbf5c","07ecbdbe-b5f4-4ca2-807f-f068be209a44","15b02567-ca85-487f-894c-6118141cc3c8","ebebd744-4ce2-4337-93e2-3251e9122982","b8234eff-bcd4-459c-978b-82197d5e0900","836040ac-3b13-4012-b519-df1b4c99578b","bf5a8fe5-5d3b-4b71-bfad-c48adaa6c2d1","3a5bea8b-243b-4550-8d9f-a7ba63818ec1"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Dallas Cows","gameAttr":[],"location":"Dallas","nickname":"Cows","permAttr":[],"rotation":["fb15a060-a940-4538-b5fd-d7f39cafcc85","9de086f9-7b67-46c5-a017-89c98788d0d4","c87e335f-f547-49e6-bf27-dafe82f13f89","cab95673-f31b-4fb1-9764-25ceb03dd761","5a0b0a20-6dc3-40dd-aa1b-a325cc46dfea"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#8c8d8f","shameRuns":0,"shorthand":"COW","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":25,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":25,"seasonShamings":0,"secondaryColor":"#b2b3b5","tournamentWins":0,"underchampionships":0},{"id":"57ec08cc-0411-4643-b304-0e80dbc15ac7","card":18,"emoji":"0x1F357","level":4,"state":{"permModSources":{}},"lineup":["4542f0b0-3409-4a4a-a9e1-e8e8e5d73fcf","f4ca437c-c31c-4508-afe7-6dae4330d717","b323f897-ccf9-4200-8870-249cbe19f904","aa7ac9cb-e9db-4313-9941-9f3431728dce","80e474a3-7d2b-431d-8192-2f1e27162607","d6c69d2d-9344-4b19-85a4-6cfcbaead5d2","65273615-22d5-4df1-9a73-707b23e828d5"],"slogan":"Pase Lo Que Pase","shadows":["23110c0f-2cf9-4d9c-ab2d-634f2f18867e","66e8f8fb-22f9-4591-9435-37dc93493202","7951836f-581a-49d5-ae2f-049c6bcc575e","75e80e46-276d-4388-bb6a-da9125a0e8ad","c9a22fa4-1721-4eef-a1a6-620cdf7d377f","1448f1f3-d76f-45c1-a5c1-54f22544000b","c4cd3c19-f59b-4e03-bedb-ed3cc944982e","e919dfae-91c3-475c-b5d5-8b0c14940c41","ceac785e-55fd-4a4e-9bc8-17a662a58a38","ca709205-226d-4d92-8be6-5f7871f48e26","6bd4cf6e-fefe-499a-aa7a-890bcc7b53fa","094ad9a1-e2c7-49a0-af18-da0e3eb656ba","19af0d67-c73b-4ef2-bc84-e923c1336db5","ad1e670a-f346-4bf7-a02f-a91649c41ccb","b9a8894f-8c5a-449b-bc98-170148ba8a94","7f3f2e75-900e-40b6-846b-db585541490d","a815ba94-d521-4400-9341-8cc4cc53aec2","1675a7c5-e7ea-4bbc-9e33-0df22a838612","8e71bd8e-eaaa-401a-a80b-d06612abbc7a"],"stadium":"2841dc8c-b5a2-4b3a-a717-9e646e8e29fc","deceased":false,"eDensity":1962.3689521744172,"fullName":"Mexico City Wild Wings","gameAttr":[],"location":"Mexico City","nickname":"Wild Wings","permAttr":[],"rotation":["dddb6485-0527-4523-9bec-324a5b66bf37","089af518-e27c-4256-adc8-62e3f4b30f43","f9fe0130-4741-4103-af9c-86d85724ce54","b04a6575-fbab-41c4-9c2c-ce0c0e44400b","bd8778e5-02e8-4d1f-9c31-7b63942cc570","cfb42325-592a-4119-9b40-4cdc19959b09","7007cbd3-7c7b-44fd-9d6b-393e82b1c06e"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#d15700","shameRuns":1,"shorthand":"MCWW","winStreak":-1,"imPosition":[0.7095121490998423,0.04183296508848771],"teamSpirit":0,"totalShames":107,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":90,"seasonShamings":0,"secondaryColor":"#ee6300","tournamentWins":0,"underchampionships":0},{"id":"67c0a873-ef6d-4a85-8293-af638edf3c9f","card":-1,"emoji":"0x1F9AC","level":0,"state":{},"lineup":["c2a720da-eea1-4487-978e-873c4af5e7cc","fc05386a-1474-49e8-a219-2bcf73587f01","88e8f41d-4a4b-423c-82b4-5dac3c996006","e520cfe3-ec6e-44c6-8fb8-9afc2e966e1c","4787fada-52c5-416f-99e7-f3f6e8c1098b","eeddfe95-49b0-47ec-9f06-017b4dc444dc","3017db8b-b627-4d33-a51c-e4045a17887c","dc07e2aa-6f4c-4108-96ed-ce4ce40672fd","8c441ccc-fd46-4ac4-89ed-155041d2479d"],"slogan":"Ya Herd!","shadows":["bd04c810-c34c-49ec-b016-fe1142a4d5b1","e02fd171-5a6a-45d8-9c5f-ecd80e649bc1","9acfc781-f184-469b-9fc8-98613209561f","5205f020-63c5-4223-aa5a-3ca3f80190c1","4b0a67ea-3e28-4a0b-acab-7e0b988e5ec5","5257d037-f4cb-4e34-96ad-f217e056cf4d","377a983e-42ce-4a06-be20-2755cb9c504c","9a3b2a51-534d-4eea-9283-7c69ca1e90ea","6f1a5558-652a-4566-86d7-fb91e8a6cd44","2b871daa-f34e-4c91-94fc-0ee67c871bdf","25ef2a78-7487-41fd-b01f-a7cf137abe06"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Busan Bison","gameAttr":[],"location":"Busan","nickname":"Bison","permAttr":[],"rotation":["b1f27dab-c048-4670-9b70-e2db7f978d0c","6654fe8a-85d4-4bfb-9cf6-578965fe8dab","81d9839a-62a2-4047-8d80-2d969be6813d","ac4a94bf-ef7d-4b88-b66e-0e0864e673b3","bb35ae12-0094-4c8b-82be-fb77de360b15"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#4971e9","shameRuns":0,"shorthand":"BSN","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":36,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":36,"seasonShamings":0,"secondaryColor":"#4971e9","tournamentWins":0,"underchampionships":0},{"id":"698cfc6d-e95e-4391-b754-b87337e4d2a9","card":0,"emoji":"0x1F3C5","level":0,"state":{},"lineup":["e16c3f28-eecd-4571-be1a-606bbac36b2b","86d4e22b-f107-4bcf-9625-32d387fcb521","c0732e36-3731-4f1a-abdc-daa9563b6506","89ec77d8-c186-4027-bd45-f407b4800c2c","d1a198d6-b05a-47cf-ab8e-39a6fa1ed831","c675fcdf-6117-49a6-ac32-99a89a3a88aa","efafe75e-2f00-4418-914c-9b6675d39264","38a1e7ce-64ed-433d-b29c-becc4720caa1","5bcfb3ff-5786-4c6c-964c-5c325fcc48d7","a1628d97-16ca-4a75-b8df-569bae02bef9","864b3be8-e836-426e-ae56-20345b41d03d","26545c1f-bdb9-41f5-bced-45238b45e9fc","195dc422-98b6-4c34-a0b9-7b7038331216","04e14d7b-5021-4250-a3cd-932ba8e0a889","f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1","88cd6efa-dbf2-4309-aabe-ec1d6f21f98a"],"slogan":"Preservation","shadows":["fb9bedb4-9c8b-4520-a062-23fba0d5f05f","f3561c54-11c3-4b3d-9182-85386471b2eb","98d233de-9c3f-4100-a343-5297b7fdeb92","c1dddfc5-6ad6-4b31-9e52-98236c533876","8ed5b2ff-7725-4c6c-9845-0b821d8aa46f","e5fda841-73bf-4fd1-b460-2702d6d55115","9bd4e070-a5d9-4f8d-92eb-fb2b469f30a7","b9615747-4710-4dbe-a68e-b376c64dc10a","0ac5f3da-8acf-4b8e-84ad-99fa20fd2105","e7a4916c-9a0b-4e34-a193-d5a50e0224a8","c817c6cc-8574-4857-83f1-4a311fa89258","da77e846-8944-4618-83a3-9bf28cd77e5d","e73c4330-4a9f-44d0-b74e-ddebfed57bf4","017ca93f-207d-45e2-a262-ba4426cacad7","1ee36c1a-b499-4d8c-953c-5acdfdc1ea1a","13420395-a3da-4816-978c-e44891cdf2d4","3c6d933d-3f2a-406a-8990-61ebff36c2c6","6669d0e6-7093-4c3e-a106-dd7dd39dc1d8","fe70fe7e-7f59-451c-8ecb-081549b8397c","8c534d32-d5d0-42c9-bc5f-3df410e04442","b49fd6aa-277f-46be-89d4-e31ac38585e4","6056e55e-2fdb-4a9c-854b-28c8aa1ac2bb","a3c28084-ded5-47ec-b923-c06623cba0f9","a9624b16-599a-4aff-bdfd-bff91cadf377","6254745d-f1d0-4694-989b-c9860933573f","e6532542-d946-4815-8cce-3485398de1b0","1865f29a-f39e-445b-9c56-79367d51c5c3","96fc9013-26b2-4797-8637-c7ec9f2783c8","a772e638-f5ee-4e29-8424-9301ba93d5b0","aea2b440-6d51-4b3e-99bd-c2dccc3ce92e","227fed15-dcee-41f9-9d9b-c78abb3000a3","e443a8f1-ccc8-45b3-adfa-6753b386425f","d7506068-f1b6-4871-9911-6b16b8d9600a","1da51b71-2ab3-44f4-8990-528300433497","b793b547-6b83-4e4a-ba80-81f1ef6d9e07","0de5fe43-f9e8-49e1-abf7-66864fcdc033","9ff1ca42-35f6-4d3e-94d6-0b1fd87fa3ef","f820420b-fd81-45f7-bc45-460d7c79b174","ac3629c2-0d47-45dd-82d3-386a6f2babea","6ecdd09c-178c-4ba3-a0ec-e8a4e4644fda","aac4947f-4101-4fa4-b71c-090b1cad88d3","232bc0e6-d84f-48c5-85c6-0a2615a1d102","2a5a6882-ecd9-44ab-a334-0d14fcc4ed56","8592446a-51fe-41d1-876f-c8cf1bde28c4","d42b85b6-2e8b-4377-8081-80bbf3402e7f","aa616f88-8504-466c-928c-c81579b33a63","0fc6095a-3840-4359-b4b7-a13111ce824a","db4da48b-bf66-41e7-9009-fb3a07239785","3546f652-27a8-4e05-9ebf-337d148032e2","194a78fd-3aa7-4356-8ba0-b9fdcbc0ea85","b8427094-f525-4b4c-aec0-ba70e95d3ce4","b971e3b8-6135-4023-8469-781c30c36f6e","f4b7837d-839f-42eb-a6c4-daf58822f5a5","7e79509f-1b18-4a8d-a8ca-f27f75538774","ddbff374-f9b9-46ab-8902-3f95e6d87f8f"],"stadium":"73e2e5f9-6689-4e98-b061-c843a619d671","deceased":false,"eDensity":0,"fullName":"Vault Legends","gameAttr":[],"location":"Vault","nickname":"Legends","permAttr":["FORGERY","HEIST_EXPERT"],"rotation":["2efb1f0a-72de-4178-9e53-35cd3e9f7798","560adb76-796d-40c8-9869-937994cd7eed","fedbceb8-e2aa-4868-ac35-74cd0445893f","de21c97e-f575-43b7-8be7-ecc5d8c4eaff","11de4da3-8208-43ff-a1ff-0b3480a0fbf1","5eac7fd9-0d19-4bf4-a013-994acc0c40c0","a253facf-a54a-493e-b398-cf6f0d288990","ef9f8b95-9e73-49cd-be54-60f84858a285","b082ca6e-eb11-4eab-8d6a-30f8be522ec4"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#c5c5c5","shameRuns":0,"shorthand":"VauL","winStreak":-1,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":2,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#fff8c6","tournamentWins":0,"underchampionships":0},{"id":"70eab4ab-6cb1-41e7-ac8b-1050ee12eecc","card":-1,"emoji":"0x1F4A1","level":null,"state":{},"lineup":["405dfadf-d435-4307-82f6-8eba2287e87a","740d5fef-d59f-4dac-9a75-739ec07f91cf","ad8d15f4-e041-4a12-a10e-901e6285fdc5","d86f836e-4edf-4dbd-9743-14f30461ee14","7a75d626-d4fd-474f-a862-473138d8c376","5ca7e854-dc00-4955-9235-d7fcd732ddcf","43d5da5f-c6a1-42f1-ab7f-50ea956b6cd5","1068f44b-34a0-42d8-a92e-2be748681a6f","9be56060-3b01-47aa-a090-d072ef109fbf"],"slogan":"Power Up!","shadows":["c09e64b6-8248-407e-b3af-1931b880dbee","ac69dba3-6225-4afd-ab4b-23fc78f730fb","b082ca6e-eb11-4eab-8d6a-30f8be522ec4","dfd5ccbb-90ed-4bfe-83e0-dae9cc763f10","4f328502-d347-4d2c-8fad-6ae59431d781","0eddd056-9d72-4804-bd60-53144b785d5c","64f4cd75-0c1e-42cf-9ff0-e41c4756f22a","06ced607-7f96-41e7-a8cd-b501d11d1a7e","17397256-c28c-4cad-85f2-a21768c66e67","16aff709-e855-47c8-8818-b9ba66e90fe8"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Light & Sweet Electric Co.","gameAttr":[],"location":"Light & Sweet","nickname":"Light & Sweet","permAttr":[],"rotation":["bd8778e5-02e8-4d1f-9c31-7b63942cc570","f245f6c6-4613-40f5-bc3b-85aa9ee3cf7e","2ffbaec8-a646-4435-a17a-3a0a2b5f3e16","9ac2e7c5-5a34-4738-98d8-9f917bc6d119","0295c6c2-b33c-47dd-affa-349da7fa1760"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#f07800","shameRuns":0,"shorthand":"L&S","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":5,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ffd86e","tournamentWins":0,"underchampionships":0},{"id":"71c621eb-85dc-4bd7-a690-0c68c0e6fb90","card":-1,"emoji":"0x1f415","level":0,"state":{},"lineup":["cc455d33-7d97-40d8-b54c-884d0d756231","0dd2b929-169e-4bc9-bef5-889e5d55c9f8","392078c0-0c69-4b71-ba61-3276eb00896b","4af580b9-99e1-4b30-821e-38450a799d62","1eefc6c5-7617-40d7-b481-9113d285749b","e6198cd5-691b-4737-91d6-950fd696ba6d","4fb19f27-daff-4a64-af9e-be27a353c4c8","902f3ef9-7926-4707-84d0-2fd9293ef027","d7c02d9b-0406-4380-b064-e9c747964f26","787e7290-257c-4ec6-89e2-12b7a13cf5da"],"slogan":"Oooooh Big Stretch","shadows":["6e5dd0c5-211c-475f-9531-55ab658367a1","9f679655-40a4-4853-a6b3-59d808cacb47","9e7cf663-23a2-4fa7-a2e3-8cac6af5d9c4","4cbdc6ce-b9a9-4ab5-a4b9-f2c211236d5f","1ac63473-6150-4ade-9efd-75ad424265e5","b9e9641a-ec26-470d-9e1a-35f0518994a3","5463a339-d05b-4cbb-9a96-625e2961beec","4d9a851e-eb0c-4b62-8e59-f66e5c91711a","c3945eb7-21df-42ec-b925-5861c13ca2fe","1c891f55-551d-40da-a410-d4dbac844a0d","af161a5a-c73e-4aec-94d1-ac6b8e13b898"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Downward Dogs","gameAttr":[],"location":"Downward","nickname":"Dogs","permAttr":["WALK_IN_THE_PARK"],"rotation":["76153b7f-e712-4bb6-bc00-27c830d875fc","f833f923-8511-4e23-9c62-81b5d537db9f","877a8522-61ac-4efb-b5c3-00c6ae4f2b38","756683e4-ec2e-4169-bff8-ef8ce341cf2f","65fad91c-d783-4add-a825-dfabbc46c8e5"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#c6fab3","shameRuns":0,"shorthand":"DOG","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":40,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":40,"seasonShamings":0,"secondaryColor":"#cffabd","tournamentWins":0,"underchampionships":1},{"id":"747b8e4a-7e50-4638-a973-ea7950a3e739","card":14,"emoji":"0x1F405","level":4,"state":{"permModSources":{}},"lineup":["26cfccf2-850e-43eb-b085-ff73ad0749b8","19241aa4-38e3-45ed-9750-68f4401f80e1","b3e512df-c411-4100-9544-0ceadddb28cf","9e724d9a-92a0-436e-bde1-da0b2af85d8f","ad56f749-ee48-4a85-9827-8455aeafe276","5c983667-6d14-4393-8f15-af904d8f90f8","6e744b21-c4fa-4fa8-b4ea-e0e97f68ded5"],"slogan":"Never Look Back.","shadows":["9abe02fb-2b5a-432f-b0af-176be6bd62cf","32c9bce6-6e52-40fa-9f64-3629b3d026a8","695daf02-113d-4e76-b802-0862df16afbd","db3ff6f0-1045-4223-b3a8-a016ca987af9","6f71667b-59f2-46df-adac-a8885a4f6ac5","2a8b371f-d2eb-4be7-b89f-aff10edb4412","128a33b9-c82f-408e-9fa3-5161f5a3e297","852107b8-8cf3-4fbb-97f9-76f73bc47708","243e9c5c-5a8a-4b48-9ce6-b10ddd677ae6","d744f534-2352-472b-9e42-cd91fa540f1b","f73009c5-2ede-4dc4-b96d-84ba93c8a429","6fc3689f-bb7d-4382-98a2-cf6ddc76909d","5fc4713c-45e1-4593-a968-7defeb00a0d4","77a41c29-8abd-4456-b6e0-a034252700d2","a73427b3-e96a-4156-a9ab-844edc696fed","7cf83bdc-f95f-49d3-b716-06f2cf60a78d","04f955fe-9cc9-4482-a4d2-07fe033b59ee","7932c7c7-babb-4245-b9f5-cdadb97c99fb","37efef78-2df4-4c76-800c-43d4faf07737","58fca5fa-e559-4f5e-ac87-dc99dd19e410","2727215d-3714-438d-b1ba-2ed15ec481c0","63580bd7-6138-471c-82a5-bc9d29ffd06d","2e86de11-a2dd-4b28-b5fe-f4d0c38cd20b","fc9637a2-978d-4ea9-b632-888d6a86148a","91cfaf23-f669-47ba-8ddb-8e01016172a1","1db2f602-64b1-4a5c-8697-1932cc2c6df1","4f328502-d347-4d2c-8fad-6ae59431d781","41ad8060-2bbe-4872-a879-22cb102bb64c","b7032ab8-8186-41ea-bf35-d9269e1b1c28"],"stadium":"929fc650-948a-4736-b7a2-bb7b75902e6e","deceased":false,"eDensity":1768.2151021860304,"fullName":"Hades Tigers","gameAttr":[],"location":"Hades","nickname":"Tigers","permAttr":["FIREPROOF","SEALANT","AMBUSH","FIERY","FREE_WILL"],"rotation":["bf122660-df52-4fc4-9e70-ee185423ff93","37061380-ac95-4018-854e-c308073945e9"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#5c1c1c","shameRuns":0,"shorthand":"HAT","winStreak":1,"imPosition":[0.927515943462282,0.11195643806123376],"teamSpirit":0,"totalShames":82,"rotationSlot":0,"seasonShames":0,"championships":3,"totalShamings":113,"seasonShamings":0,"secondaryColor":"#e83622","tournamentWins":0,"underchampionships":0},{"id":"74966fbd-5d77-48b1-8075-9bf197583775","card":-1,"emoji":"0x1f98f","level":0,"state":{},"lineup":["e49b9a6c-9493-430b-83c8-11b73e26b5a4","16a2f647-1a7e-471c-b513-f4a777f966f3","23d0057e-d695-42a3-84f2-194aef957341","05bf598f-94e2-4729-8154-0130c7f212c7","3d63597a-7c2f-4616-9ec1-3a3fe8809255","b7d04fd4-3faa-4b2a-8121-0f1958d6efcd","c1508af9-10c6-4d32-b5d4-afb4ee5c291e","39dd3d32-c62e-4948-b7a2-c2cc99eba788","96ddf371-63c0-4438-9360-9a8bfc702abc"],"slogan":"Tusk Any, Tusk All","shadows":["968eb365-e525-4aa1-8389-a5f48d0a99a0","48745dd0-8560-417d-b7a0-36c7ecbdea79","e647e274-168f-4d80-9192-77a3f5f30457","90e73cd5-7f53-42fc-90f6-4852c76834cf","aef42525-591e-4cc7-b1e9-f69a10d9e5a8","f8974ad3-c874-4b69-b591-5a25c9253d2c","19a44afd-5bdd-4043-978c-c46181af6e14","5d38ed05-4801-4c5a-8dbb-6fe69689e656","78a64c2b-fc6d-4ee1-a0d8-c90836d8a36a","1805fe71-1738-4bc8-9362-0a79557e7ceb","8f450b00-334a-40a2-a445-f387937956a3"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Florence Rhinoceroses","gameAttr":[],"location":"Florence","nickname":"Rhinoceroses","permAttr":[],"rotation":["44e0f741-f121-4124-ab77-f06a8827a250","f22482e8-784a-4b6e-b5ad-06f37554b89b","1268f6db-48d3-48d0-98fd-9f36bc262634","2765a648-4993-44a4-b6ae-258ffe18c8c1","218d8499-8515-4dc6-9732-8045a2b0056e"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#e27e81","shameRuns":0,"shorthand":"RNO","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":38,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":38,"seasonShamings":0,"secondaryColor":"#e27e81","tournamentWins":0,"underchampionships":0},{"id":"774762ee-c234-4c57-90a1-e1e69db3f6a7","card":-1,"emoji":"0x1F99C","level":0,"state":{},"lineup":["a1137ba7-f057-4bd6-9627-c4624b194901","75d1c426-ff0e-4a61-a287-8f5c859a2c1b","821f79d2-1043-4a7f-9b24-086aff365b15","a4e5de44-8b68-4e49-a314-54c502647a97","49a7299b-25e7-450a-989e-d45572a62383","bcf75ee4-0d7d-4687-91b6-3e7c25c4361c","13df9295-64b3-4e4b-92e8-8c3f13a29bf4","94258859-be2b-43c0-8ec3-2a8c00c1bbd1","572e6819-29da-4855-8790-176804d93ad6"],"slogan":"Polly Wants A Dinger!","shadows":["2233b5c2-c76e-4375-b2f6-d8cb25157b04","2cf94b33-bf6f-4265-b491-84ea262456ce","bbeb737f-4ea2-497a-9e04-374828398dc5","a1e74902-cf33-46ac-adfb-ab81548f1a69","50fb00a0-cc94-494b-a946-d3926f30813d","e380b1b1-2fd8-4724-a64d-04dd25275309","d567ebf0-58da-497f-a665-820b8e496571","8b266499-5d14-4481-b3ef-60831318b90f","fd2cfc43-61f8-4ad1-818b-2ca39dec3bfe","eae40f17-6b69-452a-be73-19dc2448f8d3","227cb98b-8649-475a-baf5-db1801f7bdcc"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"São Paulo Parrots","gameAttr":[],"location":"São Paulo","nickname":"Parrots","permAttr":[],"rotation":["77d7b3b2-8059-45dd-8b10-e6bec89885f7","7e8901b7-20bf-4ae5-9d1b-30169e9d9f06","b45e72c5-63a6-43d6-bcba-de811c04f938","ded6d32a-a032-4905-88d7-cafdc70a21ee","00a68bb0-1a42-4fbc-85ba-58cacd4fcd42"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#d67c2f","shameRuns":0,"shorthand":"SPP","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":41,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":41,"seasonShamings":0,"secondaryColor":"#d67c2f","tournamentWins":0,"underchampionships":1},{"id":"7966eb04-efcc-499b-8f03-d13916330531","card":0,"emoji":"0x2728","level":1,"state":{"permModSources":{}},"lineup":["8c8cc584-199b-4b76-b2cd-eaa9a74965e5","17392be2-7344-48a0-b4db-8a040a7fb532","44c92d97-bb39-469d-a13b-f2dd9ae644d1","94f30f21-f889-4a2e-9b94-818475bb1ca0","070758a0-092a-4a2c-8a16-253c835887cb","2ffbaec8-a646-4435-a17a-3a0a2b5f3e16","ac57cf28-556f-47af-9154-6bcea2ace9fc","9be56060-3b01-47aa-a090-d072ef109fbf"],"slogan":"As Above, So Below","shadows":["945974c5-17d9-43e7-92f6-ba49064bbc59","c6146c45-3d9b-4749-9f03-d4faae61e2c3","03b80a57-77ea-4913-9be4-7a85c3594745","1af239ae-7e12-42be-9120-feff90453c85","7b779947-46ef-4b77-be37-962d60875647","f617c1ef-d676-4119-8359-a5c3ac3bef51","11a2a8a7-8c92-447b-9448-2ec5ac93bf65","450e6483-d116-41d8-933b-1b541d5f0026","ac69dba3-6225-4afd-ab4b-23fc78f730fb","1ded0384-d290-4ea1-a72b-4f9d220cbe37","82733eb4-103d-4be1-843e-6eb6df35ecd7","db53211c-f841-4f33-accf-0c3e167889a0","b77dffaa-e0f5-408f-b9f2-1894ed26e744","3205f4ff-7050-470b-8970-8d7ea56564bc","e4f1f358-ee1f-4466-863e-f329766279d0","5954374a-8b46-4d25-8524-2a6ad45328b5","51dab868-820b-4969-8bba-bde0be8090d7","a4133ba6-0299-4953-8148-e64584b433a6"],"stadium":"2a1a52b3-9759-44aa-ba49-b1437396d895","deceased":false,"eDensity":394.97561569148655,"fullName":"Yellowstone Magic","gameAttr":[],"location":"Yellowstone","nickname":"Magic","permAttr":["O_NO"],"rotation":["8adb084b-19fe-4295-bcd2-f92afdb62bd7","aa6c2662-75f8-4506-aa06-9a0993313216","09f2787a-3352-41a6-8810-d80e97b253b5","24f6829e-7bb4-4e1e-8b59-a07514657e72"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#bf0043","shameRuns":0,"shorthand":"YELL","winStreak":-1,"imPosition":[0.33439374154760393,0.6084452638794811],"teamSpirit":0,"totalShames":107,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":93,"seasonShamings":0,"secondaryColor":"#f60f63","tournamentWins":0,"underchampionships":1},{"id":"7fcb63bc-11f2-40b9-b465-f1d458692a63","card":-1,"emoji":"0x1F579","level":null,"state":{},"lineup":["74b0974a-f827-4934-9dd0-2020728bd4cc","3bf8713b-8886-4fc4-983e-e2c72bef7b95","2aee32f9-a5bc-4f95-932c-cf7492d09374","b357fbf4-533e-4f2c-8616-a576e9954795"],"slogan":"what","shadows":[],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Real Game Band","gameAttr":[],"location":"Game Band","nickname":"Game Band","permAttr":[],"rotation":["a11242ae-936a-46b4-9101-be2cabafeed4"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#de0000","shameRuns":0,"shorthand":"TGB","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":5,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#de0000","tournamentWins":0,"underchampionships":0},{"id":"878c1bf6-0d21-4659-bfee-916c8314d69c","card":6,"emoji":"0x1F32E","level":3,"state":{"permModSources":{}},"lineup":["2e13249e-38ff-46a2-a55e-d15fa692468a","18af933a-4afa-4cba-bda5-45160f3af99b","0bb35615-63f2-4492-80ec-b6b322dc5450","2268f452-01b9-4e16-98bb-c07e3ce767e3","27c68d7f-5e40-4afa-8b6f-9df47b79e7dd","c755efce-d04d-4e00-b5c1-d801070d3808"],"slogan":"72° and Infinite","shadows":["0d5300f6-0966-430f-903f-a4c2338abf00","5149c919-48fe-45c6-b7ee-bb8e5828a095","937c1a37-4b05-4dc5-a86d-d75226f8490a","cc725a58-38cc-42af-9ff8-ace76541ac26","0a6b75ea-fe69-4406-be24-a149c94daa3b","ce611b45-98dc-4818-9ff9-e88237eb00f3","0ecf6190-f869-421a-b339-29195d30d37c","abbd5ec5-a15b-421c-b0c5-cd80d8907373","d8758c1b-afbb-43a5-b00b-6004d419e2c5","9820f2c5-f9da-4a07-b610-c2dd7bee2ef6","cf8e152e-2d27-4dcc-ba2b-68127de4e6a4","9786b2c9-1205-4718-b0f7-fc000ce91106","dfe3bc1b-fca8-47eb-965f-6cf947c35447","b3cf5842-83f8-4947-918e-e0c2b1dd03a6","6192daab-3318-44b5-953f-14d68cdb2722","3f1ac16f-03c4-4a0f-8daf-6e54bbe12c30","a5e4e86f-d3a8-424a-9768-2ada17a4e741","ecffa9aa-45a6-4997-a0a1-89925d89f3b6","f11ff347-a61d-4781-8971-421c4c9055ba","5c2d12bc-088f-4930-85e8-a0aef16ecff6","4bf352d2-6a57-420a-9d45-b23b2b947375","15840c01-d8ea-4823-a802-9b635c51efa2","ea9a2510-4eaf-48db-96b8-12800bdcc829"],"stadium":"36b94380-39a6-4e32-b525-3e888215798a","deceased":false,"eDensity":1271.2159001524196,"fullName":"LA Unlimited Tacos","gameAttr":[],"location":"LA Unlimited","nickname":"Tacos","permAttr":["SUN_KISSED","SUN2_PAYOUTS","ACIDIC"],"rotation":["9fd1f392-d492-4c48-8d46-27fb4283b2db","bf6a24d1-4e89-4790-a4ba-eeb2870cbf6f","d81ce662-07b6-4a73-baa4-acbbb41f9dc5"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#64376e","shameRuns":0,"shorthand":"LATA","winStreak":1,"imPosition":[0.5642915986708135,0.29155498265399715],"teamSpirit":0,"totalShames":132,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":82,"seasonShamings":0,"secondaryColor":"#b063c1","tournamentWins":0,"underchampionships":0},{"id":"88151292-6c12-4fb8-b2d6-3e64821293b3","card":-1,"emoji":"0x1f31f","level":0,"state":{},"lineup":["6e569544-524a-4428-9ef0-c21e98645517","ae9afac3-917a-4d88-9c67-1f7a0b5de8ce","396eacd5-b485-47a7-8891-38dd41a8f58c","43256f39-9db7-4bda-bc10-814a60b4ede2","f479b20b-490a-4884-b379-b9a47d117b14","5d3c5190-967f-4711-9542-9f03b6978f4a","0bfee9d6-aae4-40b1-9d61-2ffd648ace9d","0a1d221a-f4b2-4691-bb08-c7757659f6c9"],"slogan":"Alaska Forever","shadows":["70680e0a-3f31-47cb-8599-acefdf02a4ce","37543614-6bf0-4062-aa41-e6598136a8dc","c27b80b5-164e-4cb3-bb59-26c87aee79e7","4ba50817-d477-438e-af14-824ee6bfa175","59c92008-4cca-48ef-b5b3-a57000c2b08a","57b9ae39-1471-460b-94c9-75e078d9989e","09847345-5529-4766-9f4a-ed6fefc97b01","fc34c783-f8fc-4903-8653-d44630fcc368","06933fdc-a472-4f28-8023-a3661fb6e91f","4da9e7ec-7db9-49ae-a3fb-85f872c0b5ef","1145426a-c1b7-4b50-9073-0528c2f41e18"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Alaskan Immortals","gameAttr":[],"location":"Alaskan","nickname":"Immortals","permAttr":[],"rotation":["f5a2eb28-ac4e-417f-ac59-c6c4f7c40af2","69342c37-cc48-4b57-acbf-36b542980087","dc934118-1b85-4447-8087-9343e388f411","7c053e11-e9e6-4d40-94a4-f267d6862261","c793f210-4ec1-421f-afe7-7ee729799624"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#4CA8F8","shameRuns":0,"shorthand":"AL8","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":11,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":11,"seasonShamings":0,"secondaryColor":"#75BCFA","tournamentWins":0,"underchampionships":0},{"id":"8d87c468-699a-47a8-b40d-cfb73a5660ad","card":-1,"emoji":"0x1F980","level":3,"state":{"permModSources":{}},"lineup":["eaaef47e-82cc-4c90-b77d-75c3fb279e83","84a2b5f6-4955-4007-9299-3d35ae7135d3","8cf78b49-d0ca-4703-88e8-4bcad26c44b1","64c92f8d-6028-495b-b81d-32c975afb2e1","1a93a2d2-b5b6-479b-a595-703e4a2f3885","c6a19154-7438-4c4f-b786-2dfaf5951f0f","a3ea0e93-c9f5-4f85-8acd-c3c5ce8601fa","d0ffc9c2-e677-4230-a639-b0ae11650501"],"slogan":"Claws Up!","shadows":["caecbcd1-a2f7-4272-bf74-67afafc26a08","7afedcd8-870d-4655-9659-3bdfb2e17730","1ffb1153-909d-44c7-9df1-6ed3a9a45bbd","5915b7bb-e532-4036-9009-79f1e80c0e28","e6114fd4-a11d-4f6c-b823-65691bb2d288","1cded4e7-8302-49b7-a646-40a0767b6969","6cc3246f-2da2-4c7c-a46f-9b45dcbf384c","dd8a43a4-a024-44e9-a522-785d998b29c3","61bbbf34-98c1-4bf6-bd0c-56c19199b35e","d2f827a5-0133-4d96-b403-85a5e50d49e0","34e1b683-ecd5-477f-b9e3-dd4bca76db45","e1e33aab-df8c-4f53-b30a-ca1cea9f046e","ce3fb736-d20e-4e2a-88cb-e136783d3a47","093af82c-84aa-4bd6-ad1a-401fae1fce44","7e160e9f-2c79-4e08-8b76-b816de388a98","dd6ba7f1-a97a-4374-a3a7-b3596e286bb3","4e63cb5d-4fce-441b-b9e4-dc6a467cf2fd","5fa3d749-4b1b-44f9-8b44-364b625e5b7b","54e5f222-fb16-47e0-adf9-21813218dafa"],"stadium":"cfb57d7c-4118-4b0a-85cc-4e3a51a66cb6","deceased":false,"eDensity":1036.8589675117091,"fullName":"Baltimore Crabs","gameAttr":[],"location":"Baltimore","nickname":"Crabs","permAttr":["CARCINIZATION","UNHOLEY","0"],"rotation":["b9d97990-df0b-4b8f-ac85-61c44ab29a3d","611d18e0-b972-4cdd-afc2-793c56bfe5a9","f9930cb1-7ed2-4b9a-bf4f-7e35f2586d71"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#593037","shameRuns":0,"shorthand":"BALC","winStreak":-1,"imPosition":[-0.3072072145329487,0.3762339904713442],"teamSpirit":0,"totalShames":99,"rotationSlot":0,"seasonShames":0,"championships":5,"totalShamings":101,"seasonShamings":0,"secondaryColor":"#b05c6b","tournamentWins":0,"underchampionships":0},{"id":"939db13f-79c9-41c5-9a15-b340b1bea875","card":-1,"emoji":"0x1f427","level":0,"state":{},"lineup":["65d8dc6e-a85b-49c5-b9db-9d8767aee7cc","30c659af-78e3-42e3-ae25-7f24be4fc330","dba9defe-eda5-4566-9425-2ff86bc07c80","cf7e2485-b088-4500-a133-9c956ec04796","70413b9a-2345-40ee-9bd2-407ec140b207","fd63da36-42ce-4c7f-82fa-27a0ae4eac12","7cb5ff91-eb94-417b-a407-b706417771df","a30ffd48-26a1-4b19-b0a6-862bb4642bd1","11890d82-6a0b-485c-9a73-a12ae622d98f"],"slogan":"Win Now Now!","shadows":["3648471e-93a9-4451-ae5c-840733e0ddf6","4f80dcb9-5b38-4208-baf7-f68c837e4dc9","c6e1aa86-3f03-4cf7-817b-543565951ee7","6dfb1751-4185-4768-b6a9-fe6019156c85","1965b707-c7e0-4d04-8d66-d65a7f16aeea","937cc5ef-9650-428e-b1b2-3d27bfd4c32b","539a563f-a109-4059-a07e-c73c350cfd46","57a6003e-a156-41fa-b0e4-0d9fddf6cec8","8c62190d-a884-423f-892e-7adf7e274f3c","63f006a1-c5f6-48ad-adcc-1a9451c78466","53b88d09-d9b5-47eb-a36b-ffc66e7afc1b"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Boulders Bay Birds","gameAttr":[],"location":"Boulders Bay","nickname":"Birds","permAttr":[],"rotation":["249b55a5-f751-4c1d-8b89-fe8710f713d7","88da6cac-0cac-433b-b08c-a0c7a4fb488f","b07084ce-a8c8-4430-9116-53b579991dbc","b8d39dbe-2681-4fba-b898-bd1824a14c0c","a35abefc-16db-499a-b82b-e2ef3c34ee4c"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#f5fc53","shameRuns":0,"shorthand":"BBB","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":64,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":64,"seasonShamings":0,"secondaryColor":"#f4fa6a","tournamentWins":0,"underchampionships":1},{"id":"9494152b-99f6-4adb-9573-f9e084bc813f","card":-1,"emoji":"0x1f980","level":0,"state":{},"lineup":["630fcda9-6b41-4683-ada4-0b9136b9085d","847f0c8d-f3da-4f9e-ad38-3160c63ea419","7331d26d-4872-4c7c-b790-ac5776b697bd","fa27ac29-bb67-4929-90d7-e0a14e2df73e","6eeca107-564e-4032-b2b3-bfec2a64ee27","47f0ccd8-3ee3-47c7-b00d-42416cfd1344","d082ec3b-c495-45c4-ad4b-828eefbc64b0","96661094-42a0-49ff-b799-82e7672de95d","c910527c-577d-4653-8add-1cc18bae07ae"],"slogan":"Crab and Go!","shadows":["6a054837-facf-45d8-8274-70912e343848","bcacf464-be39-4981-8dd3-cbad4ccea695","ea76bce2-cb5b-4dc4-bf82-fac7df9cccae","05516e4f-980c-48eb-a5e1-07dbf6440468","1719f12b-2750-48cc-955a-0eb453bec361","783ee79a-1b6b-4e31-8557-e9a652c4d43d","7e60da1a-f59a-4bbc-8cb9-5303a568faf2","48a80e22-4871-4d1b-a68a-cbd40dc75091","679e0b2f-fabf-4e95-a92d-73df2d9c5c7f","4822b05d-3832-406d-bb27-05687880627b","c8e8c23f-0954-4a5b-8dbe-a3b4f256b847"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Baltimore Crabs","gameAttr":[],"location":"Baltimore","nickname":"Crabs","permAttr":[],"rotation":["a807ea0c-7c58-4f7b-a11b-c6f11f3a1b02","ccb923f3-a050-4c2e-b48f-fb3d3d8d031f","a3216549-71d7-4446-8b71-8f00eb2d2baa","da6f2a92-d109-47ad-8e16-854ae3f88906","5f209091-c669-488f-b314-9c0eaccee67f"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#3F83A7","shameRuns":0,"shorthand":"CLAB","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":21,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":21,"seasonShamings":0,"secondaryColor":"#60C3F8","tournamentWins":0,"underchampionships":0},{"id":"979aee4a-6d80-4863-bf1c-ee1a78e06024","card":1,"emoji":"0x1F3DD","level":1,"state":{"permModSources":{}},"lineup":["527c1f6e-a7e4-4447-a824-703b662bae4e","b09945a7-6631-4db8-bec2-72a013fb9441","9965eed5-086c-4977-9470-fe410f92d353","7b55d484-6ea9-4670-8145-986cb9e32412","5116921f-7642-441a-9a85-40c93a1e61bd"],"slogan":"It's Island Time!","shadows":["b72a136e-bbfd-4687-a576-1ab538c8f264","03d06163-6f06-4817-abe5-0d14c3154236","0bd5a3ec-e14c-45bf-8283-7bc191ae53e4","4cd0ac8f-6c2e-4bf1-a232-a5ac6fd4a677","fc1d65f5-8916-41f1-b878-4d42a965b011","979e5105-cbe9-4f95-ad13-ce30c07f1fd5","4ecee7be-93e4-4f04-b114-6b333e0e6408","d3f27363-c21a-4a90-8407-7ee6e2a4e55f","973334aa-46d8-4415-912b-a5e267f7529b","a2483925-697f-468f-931c-bcd0071394e5","3c331c87-1634-46c4-87ce-e4b9c59e2969","62823073-84b8-46c2-8451-28fd10dff250","805ba480-df4d-4f56-a4cf-0b99959111b5","36a9863c-bf69-4ddd-bbe0-6d64b0d00e8c","cd6b102e-1881-4079-9a37-455038bbf10e","13a05157-6172-4431-947b-a058217b4aa5","fbb5291c-2438-400e-ab32-30ce1259c600","13cf5521-140f-4ad9-a998-ac8af22b9bc8","21cbbfaa-100e-48c5-9cea-7118b0d08a34","baa204bf-10c1-4a85-895a-65f97477887a","0e9ecda0-e6e2-4aa9-8659-807638a3d87b","efa73de4-af17-4f88-99d6-d0d69ed1d200","019ce117-2399-4382-8036-8c14db7e1d30","6ccc65e7-817d-4528-9757-b81d327d71e2"],"stadium":"e7431679-a45e-408f-92a8-a4db422c4925","deceased":false,"eDensity":0.9933376889207466,"fullName":"Hawai'i Fridays","gameAttr":[],"location":"Hawai'i","nickname":"Fridays","permAttr":["SINKING_SHIP"],"rotation":["80de2b05-e0d4-4d33-9297-9951b2b5c950","ccc99f2f-2feb-4f32-a9b9-c289f619d84c","12c4368d-478b-42be-b6d3-fa2e9b230f82","a5f8ce83-02b2-498c-9e48-533a1d81aebf"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#3ee652","shameRuns":0,"shorthand":"HF","winStreak":1,"imPosition":[-0.16203592735493394,0.7503446958110331],"teamSpirit":0,"totalShames":95,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":94,"seasonShamings":0,"secondaryColor":"#3ee652","tournamentWins":0,"underchampionships":0},{"id":"9a5ab308-41f2-4889-a3c3-733b9aab806e","card":-1,"emoji":"0x1F451","level":null,"state":{},"lineup":["2b5f5dd7-e31f-4829-bec5-546652103bc0","86d4e22b-f107-4bcf-9625-32d387fcb521","34267632-8c32-4a8b-b5e6-ce1568bb0639","bf6a24d1-4e89-4790-a4ba-eeb2870cbf6f","d4a10c2a-0c28-466a-9213-38ba3339b65e","32551e28-3a40-47ae-aed1-ff5bc66be879","4b6f0a4e-de18-44ad-b497-03b1f470c43c","8c8cc584-199b-4b76-b2cd-eaa9a74965e5","c755efce-d04d-4e00-b5c1-d801070d3808"],"slogan":"Oh, Sweet","shadows":["adcbc290-81c5-4da9-b4da-2e594b080710","4ecee7be-93e4-4f04-b114-6b333e0e6408","cc11963b-a05b-477b-b154-911dc31960df","e6502bc7-5b76-4939-9fb8-132057390b30","7663c3ca-40a1-4f13-a430-14637dce797a","ff5a37d9-a6dd-49aa-b6fb-b935fd670820","b7ca8f3f-2fdc-477b-84f4-157f2802e9b5","d6c69d2d-9344-4b19-85a4-6cfcbaead5d2","d23a1f7e-0071-444e-8361-6ae01f13036f"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Royal PoS","gameAttr":[],"location":"Sugar","nickname":"Royal PoS","permAttr":[],"rotation":["11de4da3-8208-43ff-a1ff-0b3480a0fbf1","333067fd-c2b4-4045-a9a4-e87a8d0332d0","ef9f8b95-9e73-49cd-be54-60f84858a285","c6e2e389-ed04-4626-a5ba-fe398fe89568","a691f2ba-9b69-41f8-892c-1acd42c336e4"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#470094","shameRuns":0,"shorthand":"RPOS","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":3,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#a959ff","tournamentWins":0,"underchampionships":0},{"id":"9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","card":17,"emoji":"0x1F575","level":5,"state":{"permModSources":{}},"lineup":["8ecea7e0-b1fb-4b74-8c8c-3271cb54f659","e111a46d-5ada-4311-ac4f-175cca3357da","432ea93d-bcb9-4f01-8d93-5d0d44fea98a","d2d76815-cbdc-4c4b-9c9e-32ebf2297cc7","a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21","b7267aba-6114-4d53-a519-bf6c99f4e3a9","c0177f76-67fc-4316-b650-894159dede45","d8ee256f-e3d0-46cb-8c77-b1f88d8c9df9"],"slogan":"Bang BANG","shadows":["f56657d3-3bdc-4840-a20c-91aca9cc360e","9397ed91-608e-4b13-98ea-e94c795f651e","07ac91e9-0269-4e2c-a62d-a87ef61e3bbe","06ced607-7f96-41e7-a8cd-b501d11d1a7e","6bac62ad-7117-4e41-80f9-5a155a434856","c9339f5e-1040-4642-a4a7-07cd36d281f8","67e435a4-62dc-4272-a2fc-b2576f9e0510","1dadee11-a11f-442a-9be3-cb0b10b86719","a68e82ac-7375-4176-b754-6175ea47dcb4","7dca7137-b872-46f5-8e59-8c9c996e9d22","446a3366-3fe3-41bb-bfdd-d8717f2152a9","97981e86-4a42-4f85-8783-9f29833c192b","b7c4f986-e62a-4a8f-b5f0-8f30ecc35c5d","b7c1ddda-945c-4b2e-8831-ad9f2ec4a608","24ad200d-a45f-4286-bfa5-48909f98a1f7","e972984c-2895-451c-b518-f06a0d8bd375","68462bfa-9006-4637-8830-2e7840d9089a","a8530be5-8923-4f74-9675-bf8a1a8f7878","e8652db0-c67a-4c8a-9acc-2951cf400cd0","6d9001ff-ba9f-40c0-9315-79feba541b65","af2303b9-9f95-4d43-b1d5-d45eba9270a7","503a235f-9fa6-41b5-8514-9475c944273f","cb9545ff-8fa6-4073-803b-f5baaf22b8c6","2b9d6b50-3ce2-4cb1-8cd1-2a0fc05733e3"],"stadium":"43fbdf41-336f-46a8-9445-eb8a6e05d811","deceased":false,"eDensity":2335.466297605448,"fullName":"Houston Spies","gameAttr":[],"location":"Houston","nickname":"Spies","permAttr":["PSYCHIC"],"rotation":["fcb08e4f-df3a-446c-ab50-58a496ac5f3f","cc933b79-9218-4693-8172-f23d4eaccdf7"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#67556b","shameRuns":0,"shorthand":"HOU","winStreak":-2,"imPosition":[0.854610860851798,-0.09283756349175627],"teamSpirit":0,"totalShames":94,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":114,"seasonShamings":0,"secondaryColor":"#9e82a4","tournamentWins":0,"underchampionships":0},{"id":"9e42c12a-7561-42a2-b2d0-7cf81a817a5e","card":-1,"emoji":"0x1F303","level":null,"state":{},"lineup":["083d09d4-7ed3-4100-b021-8fbe30dd43e8","20fd71e7-4fa0-4132-9f47-06a314ed539a","9820f2c5-f9da-4a07-b610-c2dd7bee2ef6","718dea1a-d9a8-4c2b-933a-f0667b5250e6","d89da2d2-674c-4b85-8959-a4bd406f760a","1db2f602-64b1-4a5c-8697-1932cc2c6df1","15d3a844-df6b-4193-a8f5-9ab129312d8d","3a96d76a-c508-45a0-94a0-8f64cd6beeb4","4542f0b0-3409-4a4a-a9e1-e8e8e5d73fcf"],"slogan":"We Don't Sleep. We Can't Sleep","shadows":["b60605e6-ff41-4388-a61a-fafd2175f7ad","7d388846-8f4c-407f-aacb-b844d2561c5e","e4f1f358-ee1f-4466-863e-f329766279d0","2b9f9c25-43ec-4f0b-9937-a5aa23be0d9e","9965eed5-086c-4977-9470-fe410f92d353","af6b3edc-ed52-4edc-b0c9-14e0a5ae0ee3","b348c037-eefc-4b81-8edd-dfa96188a97e","167751d5-210c-4a6e-9568-e92d61bab185"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Macchiato City","gameAttr":[],"location":"Macchiato","nickname":"Macchiato","permAttr":[],"rotation":["65273615-22d5-4df1-9a73-707b23e828d5","de21c97e-f575-43b7-8be7-ecc5d8c4eaff","09f2787a-3352-41a6-8810-d80e97b253b5","90768354-957e-4b4c-bb6d-eab6bbda0ba3","0f61d948-4f0c-4550-8410-ae1c7f9f5613"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#1f1636","shameRuns":0,"shorthand":"MC","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":10,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#b5abd1","tournamentWins":0,"underchampionships":0},{"id":"a37f9158-7f82-46bc-908c-c9e2dda7c33b","card":15,"emoji":"0x1F450","level":4,"state":{"permModSources":{}},"lineup":["d23a1f7e-0071-444e-8361-6ae01f13036f","e69db30d-74ac-4265-8da6-467b0d25694b","18f45a1b-76eb-4b59-a275-c64cf62afce0","52cfebfb-8008-4b9f-a566-72a30e0b64bf","86adac6f-c694-44ac-9560-758de7eac937"],"slogan":"We’ve Got Winning to Do. Just for You.","shadows":["16a59f5f-ef0f-4ada-8682-891ad571a0b6","3de17e21-17db-4a6b-b7ab-0b2f3c154f42","f10ba06e-d509-414b-90cd-4d70d43c75f9","2f3d7bc7-6ffb-40c3-a94f-5e626be413c9","4304bcf9-239a-4aa2-a410-56a487217a2a","f6342729-a38a-4204-af8d-64b7accb5620","3ab4ec02-17d9-4c4a-b956-aa170bf58a6e","dac2fd55-5686-465f-a1b6-6fbed0b417c5","5fbf04bb-f5ec-4589-ab19-1d89cda056bd","ecf19925-dc57-4b89-b114-923d5a714dbe","e97e9b74-8011-4415-b22c-8282f2228683","b5c95dba-2624-41b0-aacd-ac3e1e1fe828","5bd4f7dd-d5f0-4554-bbdc-8b929613fb4e","cb9e44c8-aa88-474e-8346-282270064367","07fe26d2-a478-4ef2-ba67-668d64523d2d"],"stadium":"60e78a6c-fe49-4283-8293-735e9fa7e3e9","deceased":false,"eDensity":1958.2391038517333,"fullName":"Breckenridge Jazz Hands","gameAttr":[],"location":"Breckenridge","nickname":"Jazz Hands","permAttr":[],"rotation":["ea44bd36-65b4-4f3b-ac71-78d87a540b48","9d6f3d23-bc26-40e6-93aa-0b1c693e0ba8","4b01cc3f-c59f-486d-9c00-b8a82624e620","ae81e172-801a-4236-929a-b990fc7190ce","678170e4-0688-436d-a02d-c0467f9af8c0","a3947fbc-50ec-45a4-bca4-49ffebb77dbe"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#6388ad","shameRuns":0,"shorthand":"BJAZ","winStreak":1,"imPosition":[0.5999821993746431,0.0434171030483163],"teamSpirit":0,"totalShames":105,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":106,"seasonShamings":0,"secondaryColor":"#7ba9d7","tournamentWins":0,"underchampionships":0},{"id":"a3ea6358-ce03-4f23-85f9-deb38cb81b20","card":-1,"emoji":"0x1F42E","level":null,"state":{},"lineup":["51c5473a-7545-4a9a-920d-d9b718d0e8d1","d8ee256f-e3d0-46cb-8c77-b1f88d8c9df9","9f218ed1-d793-437d-a1b9-79f88f69154d","ab36c776-b520-429b-a85f-bf633d7b081a","cf8e152e-2d27-4dcc-ba2b-68127de4e6a4","020ed630-8bae-4441-95cc-0e4ecc27253b","68f98a04-204f-4675-92a7-8823f2277075","c22e3af5-9001-465f-b450-864d7db2b4a0","26cfccf2-850e-43eb-b085-ff73ad0749b8"],"slogan":"moo","shadows":["446a3366-3fe3-41bb-bfdd-d8717f2152a9","bfd9ff52-9bf6-4aaf-a859-d308d8f29616"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Club de Calf","gameAttr":[],"location":"Decaf","nickname":"de Calf","permAttr":[],"rotation":["7aeb8e0b-f6fb-4a9e-bba2-335dada5f0a3","1e8b09bd-fbdd-444e-bd7e-10326bd57156","6a567da6-7c96-44d3-85de-e5a08a919250","f2c477fb-28ea-4fcb-943a-9fab22df3da0"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#ffc6df","shameRuns":0,"shorthand":"CdC","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":13,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ffc6df","tournamentWins":0,"underchampionships":0},{"id":"a4b23784-0132-4813-b300-f7449cb06493","card":-1,"emoji":"0x1f418","level":0,"state":{},"lineup":["b3baeb19-7d90-4edf-9c66-3b1ba396ba54","5d5f79ba-bfc8-4bf8-ad0a-d5cd02aebdfc","9a22751d-995f-48bb-a8b6-a6c56030d936","3ed1221f-ddc9-4d49-9681-510f70197b74","e7ca94f1-c950-4b9e-93aa-b9c3612f978b","56f4d8e8-ddb2-4ae7-8691-d74fd85faf50","477e5696-6496-4753-9029-6f5b815e8580","8d2b0889-5397-48cc-a70d-b0ceebedeb05","319ca206-58e9-4ac8-b15e-18c2971b26b6"],"slogan":"Never Forgive, Never Forget","shadows":["bde33ad8-98ad-4240-ac28-0c66500d0c86","b976c27b-decb-46e7-9837-3362372e9f5c","cd218f25-b4c9-4357-b3fb-a93ba81f457e","e672891d-eaf9-45cc-aee9-3349d6ea9e12","3078f22c-818b-402c-bed9-1fc080427fdf","5cb75330-9e2c-4393-a5de-71ecd19ab9a1","dfa71bb1-5c81-46ca-9c2b-ce906b6eac8d","699c9b2f-c3d8-4211-b25e-63170c2ed9a8","ad38b19e-693d-406e-8478-e9cacd739c29","bbcf6c0d-3871-4285-8956-f17b22d8371e","16b50630-d29c-4ac8-83ed-e1c03792c7d8"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Phoenix Trunks","gameAttr":[],"location":"Phoenix","nickname":"Trunks","permAttr":[],"rotation":["ee4d7a67-d43d-4747-b007-8b58c4813a5e","e5c4456f-5d1a-4c05-aa60-38610480b5f1","020414d1-6436-4f84-87ff-f1447d8f8450","a1325679-cfc0-40fe-b586-e72de2b2f7e3","e6190848-c4f5-44f8-b02f-eed2a2587fa8"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#e77732","shameRuns":0,"shorthand":"PHO","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":13,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":13,"seasonShamings":0,"secondaryColor":"#e77732","tournamentWins":0,"underchampionships":0},{"id":"a7592bd7-1d3c-4ffb-8b3a-0b1e4bc321fd","card":-1,"emoji":"0x1F95B","level":null,"state":{},"lineup":["864b3be8-e836-426e-ae56-20345b41d03d","0bb35615-63f2-4492-80ec-b6b322dc5450","1301ee81-406e-43d9-b2bb-55ca6e0f7765","7b55d484-6ea9-4670-8145-986cb9e32412","814bae61-071a-449b-981e-e7afc839d6d6","81d7d022-19d6-427d-aafc-031fcb79b29e","6c346d8b-d186-4228-9adb-ae919d7131dd","2e13249e-38ff-46a2-a55e-d15fa692468a","ee55248b-318a-4bfb-8894-1cc70e4e0720"],"slogan":"Not Milk?","shadows":["4204c2d1-ca48-4af7-b827-e99907f12d61","f56657d3-3bdc-4840-a20c-91aca9cc360e","36786f44-9066-4028-98d9-4fa84465ab9e","0eea4a48-c84b-4538-97e7-3303671934d2","12577256-bc4e-4955-81d6-b422d895fb12","a199a681-decf-4433-b6ab-5454450bbe5e"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Milk Proxy Society","gameAttr":[],"location":"Milk Substitute","nickname":"Milk Proxy","permAttr":[],"rotation":["20be1c34-071d-40c6-8824-dde2af184b4d","ea44bd36-65b4-4f3b-ac71-78d87a540b48","df4da81a-917b-434f-b309-f00423ee4967","87e6ae4b-67de-4973-aa56-0fc9835a1e1e","0c83e3b6-360e-4b7d-85e3-d906633c9ca0"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#ffffff","shameRuns":0,"shorthand":"MPS","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":7,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ffffff","tournamentWins":0,"underchampionships":0},{"id":"adc5b394-8f76-416d-9ce9-813706877b84","card":12,"emoji":"0x1F36C","level":2,"state":{"permModSources":{}},"lineup":["fa5b54d2-b488-47cd-a529-592831e4813d","60026a9d-fc9a-4f5a-94fd-2225398fa3da","fcbe1d14-04c4-4331-97ad-46e170610633","4b6f0a4e-de18-44ad-b497-03b1f470c43c","87e6ae4b-67de-4973-aa56-0fc9835a1e1e","a199a681-decf-4433-b6ab-5454450bbe5e"],"slogan":"Fresh Breath, Here We Come.","shadows":["35d5b43f-8322-4666-aab1-d466b4a5a388","5679d5de-f417-45db-ab8d-49b92e744486","849e13dc-6eb1-40a8-b55c-d4b4cd160aab","cd417f8a-ce01-4ab2-921d-42e2e445bbe2","e9a46e0c-0437-443a-be81-956665ec588e","6305e2dd-2d5d-49ed-ab47-b1b56f0e7e85","53e701c7-e3c8-4e18-ba05-9b41b4b64cda","3d5a37b6-97f9-4790-8835-69cc813f91f3","d46abb00-c546-4952-9218-4f16084e3238","6e373fca-b8ab-4848-9dcc-50e92cd732b7","7663c3ca-40a1-4f13-a430-14637dce797a","113f47b2-3111-4abb-b25e-18f7889e2d44","64f4cd75-0c1e-42cf-9ff0-e41c4756f22a","a8e757c6-e299-4a2e-a370-4f7c3da98bd1","8780e1f9-f059-4ff1-aa03-565128525188","6598e40a-d76d-413f-ad06-ac4872875bde","90c8be89-896d-404c-945e-c135d063a74e","01772798-8d45-47cb-bbb5-515832c5b590","42ad1194-9679-4d29-9028-d8e838b512d9","faff0587-55ae-49ed-9c79-a8d7cbfbdd19","1f159bab-923a-4811-b6fa-02bfde50925a","b0e7ae6b-5bd5-4c39-b344-b83cc2e466ca","ff91500d-c600-4c64-8432-96df403a1dfa","87d5d848-a1b1-4e3e-b3ab-5c47433f066e","9337389f-4301-4e30-a108-4680c02ee1fe","f7ce3cce-73bc-4c60-8e85-d58ff958f49b"],"stadium":"21ce1233-b2d6-4bd2-8d03-68a8915afdc5","deceased":false,"eDensity":802.6226027346686,"fullName":"Kansas City Breath Mints","gameAttr":[],"location":"Kansas City","nickname":"Breath Mints","permAttr":["FREE_WILL"],"rotation":["f2a27a7e-bf04-4d31-86f5-16bfa3addbe7","57290370-6723-4d33-929e-b4fc190e6a9a"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#178f55","shameRuns":0,"shorthand":"KCBM","winStreak":1,"imPosition":[0.38459344650327604,0.46072245232919484],"teamSpirit":0,"totalShames":106,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":85,"seasonShamings":0,"secondaryColor":"#178f55","tournamentWins":0,"underchampionships":0},{"id":"b024e975-1c4a-4575-8936-a3754a08806a","card":3,"emoji":"0x1F969","level":5,"state":{"permModSources":{}},"lineup":["41bdb527-1fb2-487d-8237-093958e737e4","c3dc7aa2-e27b-4859-bbf0-47ba66c03186","817dee99-9ccf-4f41-84e3-dc9773237bc8","733d80f1-2485-40f7-828b-fd7cd8243a01","17397256-c28c-4cad-85f2-a21768c66e67","cbd19e6f-3d08-4734-b23f-585330028665","042962c8-4d8b-44a6-b854-6ccef3d82716"],"slogan":"Well Done.","shadows":["a1ed3396-114a-40bc-9ff0-54d7e1ad1718","de52d5c0-cba4-4ace-8308-e2ed3f8799d0","13cfbadf-b048-4c4f-903d-f9b52616b15c","e7ecf646-e5e4-49ef-a0e3-10a78e87f5f4","732899a3-2082-4d9f-b1c2-74c8b75e15fb","7d388846-8f4c-407f-aacb-b844d2561c5e","b7ca8f3f-2fdc-477b-84f4-157f2802e9b5","35a99ff3-cf9e-4682-ba6e-0a6044aa3a4b","6fa08e08-6a95-4fbc-9d00-5c39c5302b60","dd7e710f-da4e-475b-b870-2c29fe9d8c00","3f08f8cd-6418-447a-84d3-22a981c68f16","721fb947-7548-49ea-8cbe-7721b0ed49e0","c6bd21a8-7880-4c00-8abe-33560fe84ac5","4fe28bc1-f690-4ad6-ad09-1b2e984bf30b","ebf2da50-7711-46ba-9e49-341ce3487e00","85e30695-d80c-42f0-a3df-32731942fb24","bb658caa-c3c3-46e9-9db5-d464be134e75","1b1bbc6d-b3a7-4434-8f53-eb392223b3be","2192de76-f567-4bdc-85dc-e61230a06c7f"],"stadium":"6bfd22e7-831d-4ba9-a9f9-2cf7ab00c39d","deceased":false,"eDensity":2203.0106238813414,"fullName":"Dallas Steaks","gameAttr":[],"location":"Dallas","nickname":"Steaks","permAttr":["AAA"],"rotation":["691f9ab2-9dd4-42e8-bf95-9fd01253c801","6b8d128f-ed51-496d-a965-6614476f8256","083d09d4-7ed3-4100-b021-8fbe30dd43e8","82d1b7b4-ce00-4536-8631-a025f05150ce"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#8c8d8f","shameRuns":0,"shorthand":"DAL","winStreak":1,"imPosition":[0.4188012011125213,-0.044981871881279306],"teamSpirit":0,"totalShames":77,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":105,"seasonShamings":0,"secondaryColor":"#b2b3b5","tournamentWins":0,"underchampionships":0},{"id":"b3b9636a-f88a-47dc-a91d-86ecc79f9934","card":-1,"emoji":"0x1F91D","level":null,"state":{},"lineup":["89ec77d8-c186-4027-bd45-f407b4800c2c","f0bcf4bb-74b3-412e-a54c-04c12ad28ecb","5bcfb3ff-5786-4c6c-964c-5c325fcc48d7","503a235f-9fa6-41b5-8514-9475c944273f","4ed61b18-c1f6-4d71-aea3-caac01470b5c","d2d76815-cbdc-4c4b-9c9e-32ebf2297cc7","7932c7c7-babb-4245-b9f5-cdadb97c99fb","8903a74f-f322-41d2-bd75-dbf7563c4abb","198fd9c8-cb75-482d-873e-e6b91d42a446"],"slogan":"There's no I in Cream ... or Sugar","shadows":["5e4dfa16-f1b9-400f-b8ef-a1613c2b026a","c18961e9-ef3f-4954-bd6b-9fe01c615186","ec68845f-3b26-412f-8446-4fef34e09c77","8d337b47-2a7d-418d-a44e-ef81e401c2ef","7158d158-e7bf-4e9b-9259-62e5b25e3de8","0cc5bd39-e90d-42f9-9dd8-7e703f316436","a38ada0a-aeac-4a3d-b9a5-968687ccd2f9","75f9d874-5e69-438d-900d-a3fcb1d429b3","378c07b0-5645-44b5-869f-497d144c7b35"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Cream & Sugar United","gameAttr":[],"location":"Cream & Sugar","nickname":"Cream & Sugar","permAttr":[],"rotation":["94baa9ac-ff96-4f56-a987-10358e917d91","5703141c-25d9-46d0-b680-0cf9cfbf4777","86adac6f-c694-44ac-9560-758de7eac937","126fb128-7c53-45b5-ac2b-5dbf9943d71b","a3947fbc-50ec-45a4-bca4-49ffebb77dbe"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#b89600","shameRuns":0,"shorthand":"C&SU","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":3,"seasonShames":0,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#ffd20f","tournamentWins":0,"underchampionships":0},{"id":"b63be8c2-576a-4d6e-8daf-814f8bcea96f","card":16,"emoji":"0x1F6A4","level":4,"state":{"permModSources":{}},"lineup":["006e1d32-9742-48ef-a6ba-36545e93b9e3","68f98a04-204f-4675-92a7-8823f2277075","20be1c34-071d-40c6-8824-dde2af184b4d","b7adbbcc-0679-43f3-a939-07f009a393db","1aec2c01-b766-4018-a271-419e5371bc8f","d4a10c2a-0c28-466a-9213-38ba3339b65e","8903a74f-f322-41d2-bd75-dbf7563c4abb","af6b3edc-ed52-4edc-b0c9-14e0a5ae0ee3"],"slogan":"¡Dale!","shadows":["66cebbbf-9933-4329-924a-72bd3718f321","0daf04fc-8d0d-4513-8e98-4f610616453b","0cc5bd39-e90d-42f9-9dd8-7e703f316436","d0dafd4e-fca2-4d9c-accc-d65678e7589f","97af97a7-19dc-4d18-9f72-8bd89f7eeaa8","b019fb2b-9f4b-4deb-bf78-6bee2f16d98d","c4418663-7aa4-4c9f-ae73-0e81e442e8a2","4aa843a4-baa1-4f35-8748-63aa82bd0e03","97ec5a2f-ac1a-4cde-86b7-897c030a1fa8","d5192d95-a547-498a-b4ea-6770dde4b9f5","12577256-bc4e-4955-81d6-b422d895fb12","21d52455-6c2c-4ee4-8673-ab46b4b926b4","889c9ef9-d521-4436-b41c-9021b81b4dfb","8c028571-c7e2-4130-ac5b-1ada3e1b9fe0","591c90bd-4312-4677-937d-a4a02c7354fd","1e229fe5-a191-48ef-a7dd-6f6e13d6d73f","73aa5478-ec4c-4ec3-8a96-984d57539f0c","32551e28-3a40-47ae-aed1-ff5bc66be879","63cf6eff-a376-4aa7-a4f2-507453d1a2c2"],"stadium":"a0851e18-d5d7-4749-b3c5-c6023fe1cc61","deceased":false,"eDensity":2003.5842518222962,"fullName":"Miami Dale","gameAttr":[],"location":"Miami","nickname":"Dale","permAttr":["LIFE_OF_PARTY","ELECTRIC"],"rotation":["0eddd056-9d72-4804-bd60-53144b785d5c","a38ada0a-aeac-4a3d-b9a5-968687ccd2f9","489cc1ce-82df-4fad-867d-de7d9822fedc","e376a90b-7ffe-47a2-a934-f36d6806f17d"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#9141ba","shameRuns":0,"shorthand":"MIA","winStreak":-1,"imPosition":[1,0.026991753392834855],"teamSpirit":0,"totalShames":104,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":101,"seasonShamings":0,"secondaryColor":"#cd76fa","tournamentWins":0,"underchampionships":1},{"id":"b6b5df8f-5602-4883-b47d-07e77ed9d5af","card":-1,"emoji":"0x1f3d7","level":0,"state":{},"lineup":["994b7867-3331-4f9c-a873-b4b2af8a1dbd","7e06b07d-8d11-4871-999c-5c0e0d90bfac","2f3b0b72-3237-43ab-8db3-619fd9551d80","e72b2bb4-494c-4bab-bffd-72f2686132de","56c3c9fb-519f-4a5d-841c-e46b4243e941","0fd5c4a0-a594-4231-9fa7-558a7d956ea9","bdaad731-88bd-44e9-83cb-2997c90f5312","df55515c-f9dc-4f5c-bb42-ea2914c5f76a"],"slogan":"Dig Deep!","shadows":["0afd82dc-c0f0-4d98-b46a-097ce36300cb","7780cc32-63de-4871-8dca-0e3e18485af1","21db9cde-c67e-4ffa-b04b-fa8f09ff7a0a","049ce097-3d8b-46dc-8bc0-c1069222416a","564dcf65-acad-4615-b484-bee6870b5552","55eb3b07-9f74-4ae8-9eca-3d7bd8141112","99a5e6cf-be80-4bed-8ebc-327a404ed5f8","eb2fcf60-2863-49b7-a669-3808f8cd79ac","e11fa044-daa0-4b2c-b320-91e27fb8f7cc","fc10d6b4-4f9b-4076-92af-1654e4a01d5f","77d20daf-d892-440d-ae80-b669ebe092f3"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Laredo Excavators","gameAttr":[],"location":"Laredo","nickname":"Excavators","permAttr":[],"rotation":["88deffb6-9f94-42a5-9f8c-f528b37037b4","fff76fc9-9d19-44bc-abcd-66ebddc41dd2","e9d9cd83-8e32-4372-bda5-b06181ce9424","e9df195e-176c-414d-bc84-f9ecb9e4b950","554d3663-f4a3-4b8e-ae03-1051cd065ba5"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#41260d","shameRuns":0,"shorthand":"LAR","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":9,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":9,"seasonShamings":0,"secondaryColor":"#c88c51","tournamentWins":0,"underchampionships":0},{"id":"b72f3061-f573-40d7-832a-5ad475bd7909","card":19,"emoji":"0x1F48B","level":3,"state":{"permModSources":{}},"lineup":["24e4d271-6dff-4c68-81f4-0e9ecd4dc75c","34267632-8c32-4a8b-b5e6-ce1568bb0639","114100a4-1bf7-4433-b304-6aad75904055","f2c477fb-28ea-4fcb-943a-9fab22df3da0","d259402f-98f3-44dc-b848-e432d0ae63a3","91f5298e-a2ec-4f54-a541-1818702d1b6d","c9e4a49e-e35a-4034-a4c7-293896b40c58","9f6d06d6-c616-4599-996b-ec4eefcff8b8"],"slogan":"Let's Go All The Way!","shadows":["26f01324-9d1c-470b-8eaa-1b9bfbcd8b65","7c5ae357-e079-4427-a90f-97d164c7262e","0e27df51-ad0c-4546-acf5-96b3cb4d7501","0268b35f-b19b-4b2f-90c4-89fb36403686","8997123f-b24d-4326-80c8-3c862f80f623","331d9910-0991-40c8-9345-a7cddb7465c6","ee55248b-318a-4bfb-8894-1cc70e4e0720","1e8b09bd-fbdd-444e-bd7e-10326bd57156","b390b28c-df96-443e-b81f-f0104bd37860","0f62c20c-72d0-4c12-a9d7-312ea3d3bcd1","23e78d92-ee2d-498a-a99c-f40bc4c5fe99","f1185b20-7b4a-4ccc-a977-dc1afdfd4cb9","9313e41c-3bf7-436d-8bdc-013d3a1ecdeb","3be2c730-b351-43f7-a832-a5294fe8468f","e749dc27-ca3b-456e-889c-d2ec02ac7f5f","bd24e18b-800d-4f15-878d-e334fb4803c4","2f1c87ef-c5c4-4bd8-9786-cc39473fa9df","a6ec8d00-a78f-4f6b-b51a-db9f5507cf0a","83d2e0b5-5ba6-4dfc-8a59-4dac5467436d"],"stadium":"1c1614b3-abdb-4008-98eb-12aeb8f7bb54","deceased":false,"eDensity":1325.8466437320562,"fullName":"San Francisco Lovers","gameAttr":[],"location":"San Francisco","nickname":"Lovers","permAttr":["LOVE"],"rotation":["18077848-81e0-49db-b5af-b738ef730596","167751d5-210c-4a6e-9568-e92d61bab185","db33a54c-3934-478f-bad4-fc313ac2580e","90c2cec7-0ed5-426a-9de8-754f34d59b39","c3ae0552-59e8-44bf-ba66-48a96aff35e6","d97835fd-2e92-4698-8900-1f5abea0a3b6","7158d158-e7bf-4e9b-9259-62e5b25e3de8"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#780018","shameRuns":1,"shorthand":"SFL","winStreak":-2,"imPosition":[0.8550774246138695,0.2718064438609122],"teamSpirit":0,"totalShames":112,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":99,"seasonShamings":0,"secondaryColor":"#da0000","tournamentWins":0,"underchampionships":1},{"id":"bb4a9de5-c924-4923-a0cb-9d1445f1ee5d","card":-1,"emoji":"0x1F40C","level":6,"state":{"permModSources":{}},"lineup":["750a7ba9-e595-40be-93e5-4021f1f9460d","27839754-c351-43ec-9bff-ca662a2385b9","0813d43c-b938-49da-acef-65b73f27b099","0148c1b0-3b25-4ae1-a7ce-6b1c4f289747","a5a02a4b-72a2-4eef-802c-14a1964e0dae","9a9bb4f5-d2a5-4ce2-b715-9e2c74a65502","ccd6068b-2735-4072-bfe5-571a5d57c285","3822990b-5ee0-404a-9e06-846bb29f3faf","2b5f5dd7-e31f-4829-bec5-546652103bc0","8e0b5ae4-66b7-4acd-9464-9a7cf266f1da","51dd8526-b344-4549-a4c9-91da69ce26c1"],"slogan":"Oh, Worm?","shadows":["103d788d-2c74-4edc-9299-2a1384a2776a","8605af4b-b235-43f8-81aa-6381ecdda756","72fb797a-aea8-4723-b7e4-d110c301320e","26503f2c-631a-45ba-905e-aa14471c8b8e","99766c1b-c590-4261-b1b7-3e9c7fc35608","2918be01-e1aa-4de6-8239-fe62f37769de","48d07a64-9ea2-4b9e-8046-6901ad398490","46721a07-7cd2-4839-982e-7046df6e8b66","b93f4c32-64e4-4461-b708-05f6f7f6bbc2","58c9e294-bd49-457c-883f-fb3162fc668e","f236bd5b-4ff1-4154-a3cd-7cb3c0ddaa5b","1abe7a14-2608-485c-af10-69cf0107c2a5","9fbab4df-35dd-4991-9e82-9635ab756b0f","bbf9543f-f100-445a-a467-81d7aab12236","d0ec7754-8b0a-464e-9c92-8f7289317032","5d1770ff-7366-476e-8e8c-a7b82ed095de","c3582cd2-b079-4c25-af7b-e7a709658a29"],"stadium":"8a84154e-80d7-47d5-8f56-295e9a9653d9","deceased":false,"eDensity":2672.645090645353,"fullName":"Ohio Worms","gameAttr":[],"location":"Ohio","nickname":"Worms","permAttr":["BOTTOM_DWELLER","BLACKHOLE_PAYOUTS","HEAVY_HANDED"],"rotation":["924de86d-260b-495b-80e1-57af5767703c","57a19a22-f2cd-4e59-aa84-15cb0af30ba3","73265ee3-bb35-40d1-b696-1f241a6f5966","06fdd1ed-13a0-4eb2-a7dd-4e9551235a68"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#5c4822","shameRuns":0,"shorthand":"OHWO","winStreak":-2,"imPosition":[0.30988090272646274,-0.2196713735411142],"teamSpirit":0,"totalShames":58,"rotationSlot":0,"seasonShames":0,"championships":4,"totalShamings":51,"seasonShamings":0,"secondaryColor":"#ba9c65","tournamentWins":0,"underchampionships":0},{"id":"bfd38797-8404-4b38-8b82-341da28b1f83","card":9,"emoji":"0x1F45F","level":2,"state":{"permModSources":{}},"lineup":["020ed630-8bae-4441-95cc-0e4ecc27253b","740d5fef-d59f-4dac-9a75-739ec07f91cf","fab66c3a-0ed4-4fdb-a31a-719c45b8092e","190f5b6a-c1d2-4ff3-94bb-756919bcab2b","63512571-2eca-4bc4-8ad9-a5308a22ae22","30218684-7fa1-41a5-a3b3-5d9cd97dd36b","f7847de2-df43-4236-8dbe-ae403f5f3ab3","b8ab86c6-9054-4832-9b96-508dbd4eb624","f4a5d734-0ade-4410-abb6-c0cd5a7a1c26","d47dd08e-833c-4302-a965-a391d345455c"],"slogan":"Your Kicks are Our Kicks","shadows":["d6e9a211-7b33-45d9-8f09-6d1a1a7a3c78","3531c282-cb48-43df-b549-c5276296aaa7","b056a825-b629-4856-856b-53a15ad34acb","fad06b19-42a0-47e5-82d7-6e66d4adc9a1","3e0f7f48-675b-4f72-bb40-8a8c676f0054","9416dfa3-05e9-46f3-b1df-4fa6703e9b5b","4ca52626-58cd-449d-88bb-f6d631588640","9ef09db2-da89-44a7-b41b-1348775919b8","fe3e8ba5-6687-400d-9828-2b70d63ea8be","50154d56-c58a-461f-976d-b06a4ae467f9","2859166d-6aed-4262-b05b-db85c49b3391","93502db3-85fa-4393-acae-2a5ff3980dde","5b5bcc6c-d011-490f-b084-6fdc2c52f958","2d22f026-2873-410b-a45f-3b1dac665ffd","d002946f-e7ed-4ce4-a405-63bdaf5eabb5","f36dad5f-32da-4257-af5f-04724adea2d8","17622536-acfb-4779-8394-2bde967fc634"],"stadium":"36ae955c-6264-4437-8669-3053f62992e5","deceased":false,"eDensity":864.2471304259991,"fullName":"Charleston Shoe Thieves","gameAttr":[],"location":"Charleston","nickname":"Shoe Thieves","permAttr":["TRAVELING","A","GOOD_RIDDANCE"],"rotation":["98a60f2f-cf34-4fcb-89aa-2d61fb5fba60","b1b141fc-e867-40d1-842a-cea30a97ca4f","b6aa8ce8-2587-4627-83c1-2a48d44afaee","03f920cc-411f-44ef-ae66-98a44e883291","5e4dfa16-f1b9-400f-b8ef-a1613c2b026a"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#ffce0a","shameRuns":0,"shorthand":"CHST","winStreak":-6,"imPosition":[0.5640048213852702,0.43856862564132965],"teamSpirit":0,"totalShames":115,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":80,"seasonShamings":0,"secondaryColor":"#ffce0a","tournamentWins":0,"underchampionships":0},{"id":"c19bb50b-9a22-4dd2-8200-bce639b1b239","card":-1,"emoji":"0x1f69a","level":0,"state":{},"lineup":["2445448d-4dd2-49ad-a07d-6845748defb8","ef13464d-de39-4b3f-8fae-2cb5037c3020","99ab095d-7871-4831-b9d6-cff6bb0a930e","d4223cc0-1aa0-4734-8a11-dc53df2569ef","9b751c3c-6c40-4a20-bd00-3a1011802ee0","bf303491-d766-4aee-a3eb-716865ce419c","99987899-7e13-44b2-a13c-462d224840ab","6bb5de07-1ae7-4fca-b11d-2eb109a91bc1","5b13308b-0658-4c94-ab8d-5b72a0d545ed"],"slogan":"Drive it the Distance.","shadows":["28ca3343-f315-4a39-a74f-e2423b4faf08","6a1f903d-ab41-4dcc-89f7-2be012347756","3ad0aba7-2fba-4d49-9e1a-74e2fe06e8ea","ea232e74-96a9-4549-a511-1077a1a7d95b","b4a0b0f6-9e2d-4a18-bcdc-969dc211703a","a828c956-c71c-4c4b-aa7e-daf561b3a288","2652215f-b55b-487a-9d13-3d3731cde2e0","075159d5-b775-4c0b-8620-9552b956f85c","91cd8cf9-f95c-4514-ba2c-79e5d8fcc8d4","965af8b3-6397-407c-adf9-febb94b401df","81efd97c-ba37-4b00-b9af-1a3956a327bb"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Minneapolis Truckers","gameAttr":[],"location":"Minneapolis","nickname":"Truckers","permAttr":[],"rotation":["a64d05fd-2531-4545-b3d8-d8152b3b5057","83d41f6d-f9ca-4554-b869-c5367bd4ebf9","53387b25-4e80-4c14-9f39-213b132131e1","c9d650be-c41b-4a4e-8987-9fea86b0a40a","c0de2b90-387f-4b56-9570-b83342f2e50d"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#975D11","shameRuns":0,"shorthand":"TRK","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":11,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":11,"seasonShamings":0,"secondaryColor":"#D68419","tournamentWins":0,"underchampionships":0},{"id":"c6c01051-cdd4-47d6-8a98-bb5b754f937f","card":-1,"emoji":"0x1F991","level":null,"state":{},"lineup":["493a83de-6bcf-41a1-97dd-cc5e150548a3","d74a2473-1f29-40fa-a41e-66fa2281dfca","8604e861-d784-43f0-b0f8-0d43ea6f7814","bd4c6837-eeaa-4675-ae48-061efa0fd11a","31f83a89-44e3-47b7-8c9e-0dfdcd8bd30f","7310c32f-8f32-40f2-b086-54555a2c0e86","40db1b0b-6d04-4851-adab-dd6320ad2ed9","18798b8f-6391-4cb2-8a5f-6fb540d646d5","4941976e-31fc-49b5-801a-18abe072178b","98a98014-9636-4ece-a46a-4625b47c65d5","afc90398-b891-4cdf-9dea-af8a3a79d793","f7715b05-ee69-43e5-a0e5-8e3d34270c82","25376b55-bb6f-48a7-9381-7b8210842fad"],"slogan":"Hey now, you're a Hall Star","shadows":[],"stadium":null,"deceased":false,"eDensity":0,"fullName":"The Hall Stars","gameAttr":[],"location":"The Hall","nickname":"Hall Stars","permAttr":["SQUIDDEST","UNFLAMED","TRIBUTE","LOYALTY","ESCAPE","CONTAINMENT","RETIRED","FAIRNESS"],"rotation":["3af96a6b-866c-4b03-bc14-090acf6ecee5"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#5988ff","shameRuns":0,"shorthand":"TheH","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":1,"seasonShames":0,"championships":0,"totalShamings":1,"seasonShamings":1,"secondaryColor":"#5988ff","tournamentWins":0,"underchampionships":0},{"id":"c73b705c-40ad-4633-a6ed-d357ee2e2bcf","card":10,"emoji":"🏋️‍♀️","level":5,"state":{"permModSources":{}},"lineup":["da67c196-4cac-428b-8c18-2a411055ea35","51c70634-c50a-47a9-bc11-60fa72329875","fa40d6ea-e24b-4422-9df1-0643ab202c5f","077d4fd6-6396-46bd-9f85-3d6938f5657e","9c23702b-f0c2-4668-8bcc-61bbdbbfd93e","2d07ccd0-cac7-4ccd-bab6-a078bb983174","8f357c9c-521a-4062-a771-2e547746a164","97f5a9cd-72f0-413e-9e68-a6ee6a663489","56eba639-ea73-474d-bcee-ec68c0528b02"],"slogan":"Heart and Swole","shadows":["89c8f6bf-94a2-4e56-8412-bdb0182509a9","c549f280-82ba-4d8e-a4ce-c49e56461fb6","54c724af-b163-4319-93c7-fdcafbbcc990","800ac627-0334-41d5-98df-c51a47e46aef","d5ef3cfa-b9a6-4e16-a3df-6c236e8bdb3b","7088350c-905b-4ac0-a982-64191ecaecaa","831b1121-ca14-4dbb-afee-56660bb11f0c","7aeb8e0b-f6fb-4a9e-bba2-335dada5f0a3","f90c398b-eef0-40cb-8b07-302a759e8bd1","6833ca7d-910e-48f5-b25d-2886cdb08d0e","f106291a-5935-449e-b075-75e62d1d1b07","3188d641-4fcc-4d5e-887d-5f454b4c4ff8","b60605e6-ff41-4388-a61a-fafd2175f7ad","b2eb76eb-2c34-4184-b87d-4620faec8ad8","f5807c24-b644-4498-a62c-f4a531fe79c9"],"stadium":"7f09624b-b3cf-4493-b768-adf090bebf21","deceased":false,"eDensity":2496.98683250914,"fullName":"Tokyo Lift","gameAttr":[],"location":"Tokyo","nickname":"Lift","permAttr":["POPCORN_PAYOUTS"],"rotation":["88dde1fd-c845-46a8-b1f6-1505fa0c11b8","6a567da6-7c96-44d3-85de-e5a08a919250","8a6fc67d-a7fe-443b-a084-744294cec647","da785088-cf49-4fc2-b2e3-3c1455d875e2"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#e830ab","shameRuns":0,"shorthand":"TKL","winStreak":-2,"imPosition":[0.5639175064628478,-0.15132027428556236],"teamSpirit":0,"totalShames":75,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":49,"seasonShamings":0,"secondaryColor":"#e830ab","tournamentWins":0,"underchampionships":0},{"id":"ca3f1c8c-c025-4d8e-8eef-5be6accbeb16","card":13,"emoji":"0x1F525","level":1,"state":{"permModSources":{}},"lineup":["20c529b3-947e-4a77-b56f-69fe25fb3717","ad8d15f4-e041-4a12-a10e-901e6285fdc5","c182f33c-aea5-48a2-97ed-dc74fa29b3c0","94baa9ac-ff96-4f56-a987-10358e917d91","5831a238-8efe-4d53-a483-1ad681e50ac3","5ff66eae-7111-4e3b-a9b8-a9579165b0a5","20e13b56-599b-4a22-b752-8059effc81dc","7fed72df-87de-407d-8253-2295a2b60d3b"],"slogan":"We Are From Chicago","shadows":["a7edbf19-caf6-45dd-83d5-46496c99aa88","7e4f012e-828c-43bb-8b8a-6c33bdfd7e3f","64aaa3cb-7daf-47e3-89a8-e565a3715b5d","bfd9ff52-9bf6-4aaf-a859-d308d8f29616","97387580-1f7c-49ea-a9e7-44be89329130","21555bfb-6aed-4510-863a-801be3b6d997","43d5da5f-c6a1-42f1-ab7f-50ea956b6cd5","f071889c-f10f-4d2f-a1dd-c5dda34b3e2b","3c051b92-4a86-4157-988a-e334bf6dc691","c8de53a4-d90f-4192-955b-cec1732d920e","1513aab6-142c-48c6-b43e-fbda65fd64e8","316abea7-9890-4fb8-aaea-86b35e24d9be","ee29020f-1964-4fce-80f4-35a5e7edfb95","88ca603e-b2e5-4916-bef5-d6bba03235f5","e0839b6c-70fd-4228-ae3a-60c713d00d09","a1e85b16-0898-42f1-b80c-2aa19a57dc92","5b79090e-f413-463d-899d-738667e0aa41","6f39a2f9-76d7-44fd-8a6d-00f87d7bb004","1dc1b683-baf8-4a3d-9038-20eaac71f5d8"],"stadium":"06c991f6-4ef3-4a9a-8254-8a2bc7d3a1e1","deceased":false,"eDensity":357.8635823993027,"fullName":"Chicago Firefighters","gameAttr":[],"location":"Chicago","nickname":"Firefighters","permAttr":[],"rotation":["63df8701-1871-4987-87d7-b55d4f1df2e9","b59cdb56-4a85-476f-b775-62204eefbe92","520e6066-b14b-45cf-985c-0a6ee2dc3f7a","e4e4c17d-8128-4704-9e04-f244d4573c4d","62ae6aa9-e346-4faa-b07c-1f7623580015","5f3b5dc2-351a-4dee-a9d6-fa5f44f2a365"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#8c2a3e","shameRuns":0,"shorthand":"CHIF","winStreak":12,"imPosition":[-0.12567384918886051,0.6214057873489476],"teamSpirit":0,"totalShames":95,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":96,"seasonShamings":0,"secondaryColor":"#d13757","tournamentWins":0,"underchampionships":0},{"id":"cbd44c06-231a-4d1a-bb7d-4170b06e566a","card":-1,"emoji":"0x1f494","level":0,"state":{},"lineup":["81fcb973-5fae-40a5-b226-0f48291ffc78","a9991db0-5958-45d0-bb2a-412d77879c73","9a8e8fa0-96fa-41c2-9753-0c3d965dcf83","0fbffd5c-cf7c-49c0-a579-4daec23bff4d","d934fec9-1a2a-47f9-a33c-fc5c27f69a84","ff85c3aa-6177-4852-bc06-8ab32e6d6820","d0ffac42-a19b-404e-b54f-8bb5a6f14ad3","edf4c854-fc1c-40c5-8573-4579143f2227","23f9bba8-294e-4396-97fa-58cb2fc3c96d"],"slogan":"Steal Hearts Steal Home","shadows":["bc348787-13f8-4d26-bdaa-1aeeb0c67784","95f704c7-d9e2-4df5-b0a5-da2c17052ed6","6c93f393-3694-49be-b8d8-f38a739a0399","1d550329-ac65-488e-a8b7-c131832eb601","4a276ef6-3758-44f2-96c2-dc1ba289b7ae","e05906bc-523a-4ac5-9e41-4dd526900a54","d007d328-88f0-42e2-a361-6220c47250c0","1744f78f-783d-421a-831f-246b99586abc","7a476b05-5713-433c-8779-20be74551267","b101de99-cbb0-483b-a5db-3d00e4cfe533","4e686fed-33c5-40c7-8f82-7f5ff764d728"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Oklahoma Heartthrobs","gameAttr":[],"location":"Oklahoma","nickname":"Heartthrobs","permAttr":["LOVE"],"rotation":["a17d99aa-336b-4900-9aba-2398891b0cbd","753a6d17-3fc0-4572-879b-d0d51a1f86fe","9be7e1b5-a07c-4657-a7c2-5bf9a16dc0f5","271887b0-cf32-43da-9bc9-a770c10b6ed8","9f6ee119-ec92-4fc1-ae93-92788bf6e4cf"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#fdc846","shameRuns":0,"shorthand":"HRT","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":16,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":16,"seasonShamings":0,"secondaryColor":"#fadc92","tournamentWins":0,"underchampionships":0},{"id":"d0762a7e-004b-48a9-a832-a993982b305b","card":-1,"emoji":"0x1f40b","level":0,"state":{},"lineup":["ded76037-e012-491a-94e0-2fd9f455f043","2220ba27-a0cc-4bad-86b2-268d087ec243","fda7a82e-fe5f-42ea-b9f5-2befe49678e2","e1725cb7-d09e-4322-a8ca-80c6918beff8","6e0f7e7e-caca-48fd-a0e3-18fccbbf84eb","e6ca8904-c2d4-4d7c-b766-4e83e7c98abd","a489790b-f2b6-40fe-aa1b-f5f82ff5e761","647d998d-c8b8-464c-9865-7582e4329dc7","8abc1cbb-9014-43f9-bc72-2693f1dd62dd"],"slogan":"Kelp Wanted!","shadows":["d1398033-4efe-401e-9042-b0be90cc5d3d","4facfdac-c86f-47c9-9654-2d69ad9c60d6","549cdfc9-2965-4e9a-9279-d21a604c7908","b3221404-0cb4-47df-85ff-764b0886fd9d","e37f92d8-47d5-4a67-8040-ff5c340f9963","6803d6ac-a945-4ace-9dfb-2fb14279a0bd","11938987-8508-4a4e-ae24-bcc9f691eb5a","946a95ed-5d01-4a85-94cc-441302cef30d","8c5db31e-21b9-4a11-acb7-3e89f3756e1a","9604cad7-c029-4384-874c-f38c98933632"],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Mallorca Whales","gameAttr":[],"location":"Mallorca","nickname":"Whales","permAttr":[],"rotation":["71a20f3a-2f49-486e-bef1-987acbff676f","7dae08e0-0763-424f-9e88-3f4ef95c3ec4","16a38c85-92fd-4db9-a186-395e84af2b26","8a009f3f-e9ff-47e2-bd1c-c6835aaebb48","6c9a5139-fb08-4037-a677-e7dfd7b52cdf"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#025C30","shameRuns":0,"shorthand":"KELP","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":13,"rotationSlot":0,"seasonShames":0,"championships":0,"totalShamings":13,"seasonShamings":0,"secondaryColor":"#02A355","tournamentWins":0,"underchampionships":0},{"id":"d2634113-b650-47b9-ad95-673f8e28e687","card":-1,"emoji":"0x1F52E","level":null,"state":{},"lineup":["13032f07-10bf-48eb-a23e-f4117d246868","aadbe486-6bf2-42bd-8418-dbd1aa36edd9","3adb039d-0875-44f4-914f-1ef9ee4c2120","ab04ba22-69b1-43ca-a307-345e460d795a","e181ad4a-dd09-4873-ae68-b50518d20722","b0045559-81cf-42e0-87d4-3bbbd3ce0bce","0892da6e-c9d1-494f-84bc-0c1f879018c0","5a39b2dd-23c4-4a6f-a645-99b0b1992585","bbdeb3e2-cd5a-46ff-9a69-fd06b1482710"],"slogan":"Derive the Stars","shadows":["944d939e-68b8-4259-b9cd-46b4ac9789c7","a25acf3a-6d2c-4485-975b-a04549d084f3","8cfb7ee9-cfa4-445c-830e-2723797fa7b3","94e1e162-9d2e-4407-803a-19bcee369933","ac13264f-af4e-4423-9e4a-43499a0f8227","5e3b1e6b-6fef-4572-80c8-f9479335ea36","14ff5a36-c39f-4910-affb-2a9ea28e09b0","dab126a9-3310-4f6e-a3ba-3e3044a0feec"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Society Data Witches","gameAttr":[],"location":"Society","nickname":"Data Witches","permAttr":["AFFINITY_FOR_CROWS"],"rotation":["e1651116-4533-4b39-8931-62d023e21579","2d5ac274-96fd-471b-8028-f4d7b42d8313","be4f288b-ef87-4e8e-b8ee-20eec18e80b8","e11ff57a-ec5d-4d59-81a3-ffe3529b0e61","92ff8e42-dcca-4a94-a83d-24d2900b811a"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#691a8f","shameRuns":1.5,"shorthand":"SIBR","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":1,"rotationSlot":11,"seasonShames":1,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#bc60e2","tournamentWins":0,"underchampionships":0},{"id":"d6a352fc-b675-40a0-864d-f4fd50aaeea0","card":-1,"emoji":"0x1f5bc","level":0,"state":{"redacted":false},"lineup":[],"slogan":"Win or Lose, Draw!","shadows":[],"stadium":null,"deceased":true,"eDensity":0,"fullName":"Canada Artists","gameAttr":[],"location":"Canada","nickname":"Artists","permAttr":[],"rotation":[],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#04B1AA","shameRuns":0,"shorthand":"CART","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":8,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":8,"seasonShamings":0,"secondaryColor":"#04DFD5","tournamentWins":0,"underchampionships":0},{"id":"d8f82163-2e74-496b-8e4b-2ab35b2d3ff1","card":-1,"emoji":"0x274C","level":null,"state":{},"lineup":["678170e4-0688-436d-a02d-c0467f9af8c0","cbd19e6f-3d08-4734-b23f-585330028665","a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21","03b80a57-77ea-4913-9be4-7a85c3594745","766dfd1e-11c3-42b6-a167-9b2d568b5dc0","32c9bce6-6e52-40fa-9f64-3629b3d026a8","04931546-1b4a-469f-b391-7ed67afe824c","817dee99-9ccf-4f41-84e3-dc9773237bc8","2f3d7bc7-6ffb-40c3-a94f-5e626be413c9"],"slogan":"We've got a Shot!","shadows":["9ba361a1-16d5-4f30-b590-fc4fc2fb53d2","60026a9d-fc9a-4f5a-94fd-2225398fa3da","cd417f8a-ce01-4ab2-921d-42e2e445bbe2","a647388d-fc59-4c1b-90d3-8c1826e07775","57290370-6723-4d33-929e-b4fc190e6a9a","c9e4a49e-e35a-4034-a4c7-293896b40c58","6b8d128f-ed51-496d-a965-6614476f8256","8b53ce82-4b1a-48f0-999d-1774b3719202","138fccc3-e66f-4b07-8327-d4b6f372f654"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Inter Xpresso","gameAttr":[],"location":"Xpresso","nickname":"Xpresso","permAttr":[],"rotation":["ae4acebd-edb5-4d20-bf69-f2d5151312ff","dddb6485-0527-4523-9bec-324a5b66bf37","5eac7fd9-0d19-4bf4-a013-994acc0c40c0","03f920cc-411f-44ef-ae66-98a44e883291","73265ee3-bb35-40d1-b696-1f241a6f5966"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#8a2444","shameRuns":0,"shorthand":"IE","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":15,"seasonShames":0,"championships":0,"totalShamings":1,"seasonShamings":1,"secondaryColor":"#e6608b","tournamentWins":1,"underchampionships":0},{"id":"d9f89a8a-c563-493e-9d64-78e4f9a55d4a","card":-1,"emoji":"0x1F531","level":2,"state":{"permModSources":{}},"lineup":["948ce8a8-19cd-4f81-b84f-88cce02c47bc","4a95d9d8-3dcc-488d-b1cb-19690271ae4c","1f930140-9fc6-4240-9ef6-e7ac065eaefb","5a26fc61-d75d-4c01-9ce2-1880ffb5550f","5361e381-6658-488b-8236-dde6a264554f","d89da2d2-674c-4b85-8959-a4bd406f760a","0ef1bc34-64ee-4c7b-8be2-88322b407a58"],"slogan":"You Sink, We Swim","shadows":["8e1eeeb8-1b80-4781-9d85-bda3b58a45f4","3c19aa93-3f9d-4be0-ae5a-8ade100a9668","35acb6ea-9a7c-49b6-809c-59409f557651","9ae7cec4-1df0-4922-8f7a-f3b996d031e7","9f85676a-7411-444a-8ae2-c7f8f73c285c","e6768069-1db7-44d6-849b-0a281f62e855","1c55aedf-6e31-4f3c-9b17-8a84b7d15a0c","ec41d37e-2398-4e7d-aaf0-f7f52e5300c7","7815e82e-5ea4-4cef-b28b-6d8e05815c52","2b157c5c-9a6a-45a6-858f-bf4cf4cbc0bd","1986c51a-01a3-4c73-84c3-2c54c7426a83","08f9651d-d917-4152-b2c4-07a891f0aea1","d86f836e-4edf-4dbd-9743-14f30461ee14","ad8a9de2-16b8-4a95-830f-8d56474a7ead"],"stadium":"37c2743f-1a15-4e3f-bdb1-a6461df93434","deceased":false,"eDensity":564.5192664636646,"fullName":"Atlantis Georgias","gameAttr":[],"location":"Atlantis","nickname":"Georgias","permAttr":["UNDERSEA","LIGHT_HANDED","FREE_WILL"],"rotation":["0288bbb1-a994-45c3-bb8f-331fe296090d","2720559e-9173-4042-aaa0-d3852b72ab2e","bc29afc1-c954-4def-978b-a59ae5def3c3","ebe6ac02-3b83-4da8-b1e5-f89053bbd5c8"],"seasAttr":[],"weekAttr":[],"evolution":1,"mainColor":"#00dbc2","shameRuns":0,"shorthand":"ATL","winStreak":1,"imPosition":[0.019558190199904665,0.5465636485441306],"teamSpirit":0,"totalShames":64,"rotationSlot":0,"seasonShames":0,"championships":4,"totalShamings":46,"seasonShamings":0,"secondaryColor":"#5cffec","tournamentWins":0,"underchampionships":0},{"id":"e3f90fa1-0bbe-40df-88ce-578d0723a23b","card":-1,"emoji":"0x1F3F3","level":null,"state":{},"lineup":["4b3e8e9b-6de1-4840-8751-b1fb45dc5605","89f74891-2e25-4b5a-bd99-c95ba3f36aa0","e3c514ae-f813-470e-9c91-d5baf5ffcf16","80e474a3-7d2b-431d-8192-2f1e27162607","d47dd08e-833c-4302-a965-a391d345455c","32810dca-825c-4dbc-8b65-0702794c424e","4bf352d2-6a57-420a-9d45-b23b2b947375","855775c1-266f-40f6-b07b-3a67ccdf8551","c0998a08-de15-4187-b903-2e096ffa08e5"],"slogan":"We Forfeit!","shadows":["62ae6aa9-e346-4faa-b07c-1f7623580015","9397ed91-608e-4b13-98ea-e94c795f651e","fcbe1d14-04c4-4331-97ad-46e170610633","27c68d7f-5e40-4afa-8b6f-9df47b79e7dd","c549f280-82ba-4d8e-a4ce-c49e56461fb6","3a8c52d7-4124-4a65-a20d-d51abcbe6540","9c23702b-f0c2-4668-8bcc-61bbdbbfd93e","db33a54c-3934-478f-bad4-fc313ac2580e","2b157c5c-9a6a-45a6-858f-bf4cf4cbc0bd","5dbf11c0-994a-4482-bd1e-99379148ee45","c3ae0552-59e8-44bf-ba66-48a96aff35e6","7007cbd3-7c7b-44fd-9d6b-393e82b1c06e","1ffb1153-909d-44c7-9df1-6ed3a9a45bbd","f3c07eaf-3d6c-4cc3-9e54-cbecc9c08286","8adb084b-19fe-4295-bcd2-f92afdb62bd7"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"FWXBC","gameAttr":[],"location":"Flat White","nickname":"FWXBC","permAttr":[],"rotation":["defbc540-a36d-460b-afd8-07da2375ee63","6f9de777-e812-4c84-915c-ef283c9f0cde","81a0889a-4606-4f49-b419-866b57331383","20e13b56-599b-4a22-b752-8059effc81dc","21cbbfaa-100e-48c5-9cea-7118b0d08a34"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#505050","shameRuns":0,"shorthand":"FWXBC","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":1,"rotationSlot":14,"seasonShames":1,"championships":0,"totalShamings":1,"seasonShamings":1,"secondaryColor":"#b8b8b8","tournamentWins":0,"underchampionships":0},{"id":"e8f7ffee-ec53-4fe0-8e87-ea8ff1d0b4a9","card":-1,"emoji":"0x2693","level":null,"state":{},"lineup":["667cb445-c288-4e62-b603-27291c1e475d","542af915-79c5-431c-a271-f7185e37c6ae","da67c196-4cac-428b-8c18-2a411055ea35","28964497-0efe-420c-9c1d-8574f224a4e9","97f5a9cd-72f0-413e-9e68-a6ee6a663489","9c3273a0-2711-4958-b716-bfcf60857013","3e008f60-6842-42e7-b125-b88c7e5c1a95"],"slogan":"[FOGHORN SOUND]","shadows":["e7ecf646-e5e4-49ef-a0e3-10a78e87f5f4"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Heavy FC","gameAttr":[],"location":"Foam","nickname":"Heavy","permAttr":[],"rotation":["338694b7-6256-4724-86b6-3884299a5d9e","04e14d7b-5021-4250-a3cd-932ba8e0a889","2720559e-9173-4042-aaa0-d3852b72ab2e","20c529b3-947e-4a77-b56f-69fe25fb3717","a5f8ce83-02b2-498c-9e48-533a1d81aebf"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#a9e8c9","shameRuns":0,"shorthand":"HFC","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":0,"rotationSlot":7,"seasonShames":0,"championships":0,"totalShamings":1,"seasonShamings":1,"secondaryColor":"#a9e8c9","tournamentWins":0,"underchampionships":0},{"id":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","card":4,"emoji":"0x1F5E3","level":4,"state":{"permModSources":{}},"lineup":["238f8c50-311f-4486-a734-e1a17606af4d","338694b7-6256-4724-86b6-3884299a5d9e","ceb5606d-ea3f-4471-9ca7-3d2e71a50dde"],"slogan":"Talk Spit, Get Hits","shadows":["6524e9e0-828a-46c4-935d-0ee2edeb7e9a","51cba429-13e8-487e-9568-847b7b8b9ac5","24cb35c1-c24c-45ca-ac0b-f99a2e650d89","9ba361a1-16d5-4f30-b590-fc4fc2fb53d2","c90eafdf-4244-41e0-8a66-adf31d700f02","c09e66b3-5847-4cbb-aeba-2b70e7bafedb","9a031b9a-16f8-4165-a468-5d0e28a81151","750c7d44-757a-430c-9fa3-d4ca22111933","9fc3c6ae-5ed5-49ce-a75b-257ec1022a15","a1b55c5f-6d01-4ca1-976a-5cdfe18d199c","64f59d5f-8740-4ebf-91bd-d7697b542a9f","adcbc290-81c5-4da9-b4da-2e594b080710","3a96d76a-c508-45a0-94a0-8f64cd6beeb4","c31d874c-1b4d-40f2-a1b3-42542e934047","04a7c002-fcd1-4948-bb35-fd0ba383406c","c57222fd-df55-464c-a44e-b15443e61b70","c3b1b4e5-4b88-4245-b2b1-ae3ade57349e","aa612fa0-5d16-49fa-b1f6-00497c0448ad","3954bdfa-931f-4787-b9ac-f44b72fe09d7","0f6e8b92-304d-4720-b650-9af5150fa9bd","a0e7445f-2b93-4ca7-a69d-ff32593700ee","187d4a44-4deb-41f1-99d2-b013419dda69"],"stadium":"f71fd587-d20c-461c-88a4-1634a2df3f43","deceased":false,"eDensity":1549.6684102053357,"fullName":"Canada Moist Talkers","gameAttr":[],"location":"Canada","nickname":"Moist Talkers","permAttr":["HIGH_PRESSURE","H20","MODERATION"],"rotation":["e6502bc7-5b76-4939-9fb8-132057390b30","3d293d6c-3a20-43ab-a895-2b7f1f28779f","ce7d33b7-1bba-48d6-a1d1-cd0e29231be8","32f3f7f8-e178-4d79-ac58-7a73abaf60aa","28162da7-eafa-4eb1-8bc1-5a625f03ae57","8f11ad58-e0b9-465c-9442-f46991274557","36786f44-9066-4028-98d9-4fa84465ab9e","a691f2ba-9b69-41f8-892c-1acd42c336e4","27faa5a7-d3a8-4d2d-8e62-47cfeba74ff0","90768354-957e-4b4c-bb6d-eab6bbda0ba3","f741dc01-2bae-4459-bfc0-f97536193eea"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#f5feff","shameRuns":0,"shorthand":"CAN","winStreak":1,"imPosition":[-1,0.19268842442149015],"teamSpirit":0,"totalShames":109,"rotationSlot":0,"seasonShames":0,"championships":3,"totalShamings":114,"seasonShamings":0,"secondaryColor":"#f5feff","tournamentWins":0,"underchampionships":0},{"id":"ed60c164-fd31-42ff-8ae1-70220626f5a7","card":-1,"emoji":"0x1f428","level":0,"state":{},"lineup":["c7219e36-671a-4244-bf2f-f7e3a91b7afd","b510c829-2408-4908-8c12-810d39deea9a","b01d1610-105b-4e31-8b36-0ab12a51b9ae","f6e3745e-dbee-4043-8d9a-6e8aea138346","a709a6c1-f0a8-4748-9f85-d809bf5819ef","580da06f-115a-4778-b563-e27dbef75ce6","f66cb477-0542-4ea3-8276-499087399a32","55816eac-d91e-48dc-951f-43219e7587af","ecafd3c3-1b4d-4920-bd80-f1ea503235fe"],"slogan":"LOOK OUT","shadows":["edd95bea-dd51-416f-9372-22db4c39c140","ddcd33e0-b888-4cfb-b29d-70fd751b4b7b","9800d176-cf54-43fc-a21b-e622b02a967a","7d14ca3a-26f8-4e97-86d3-a512ff473a5c","cef92ffa-de37-41b8-889e-ad65d1a978b8","9fec99e5-b689-4166-9bdd-d336fa0a1b27","79ed4728-cd5a-487b-9816-1c629eacdbd8","b477519d-432e-4d14-bf66-6d309b8ca8d8","a484467d-5fb3-46e8-88d5-adb88b5bce2d","19db871f-a6ab-497c-9948-ba86df018bcb","968cd343-98f5-4a6a-a636-1ecd51df09dd"],"stadium":null,"deceased":false,"eDensity":0,"fullName":"Canberra Drop Bears","gameAttr":[],"location":"Canberra","nickname":"Drop Bears","permAttr":[],"rotation":["9bc3f1cb-af4c-434b-83e8-ff3479fbb65e","060ce5c7-93bf-4ee7-a2fb-be86736cac1e","a116f972-d10c-4c69-bac6-c98404bcc4e5","797125b9-7623-4312-a8f0-310bf0557404","48f7d036-5df9-49f0-88a0-1914cf00e730"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#c8f045","shameRuns":0,"shorthand":"GRR","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":135,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":135,"seasonShamings":0,"secondaryColor":"#c8f045","tournamentWins":0,"underchampionships":0},{"id":"f02aeae2-5e6a-4098-9842-02d2273f25c7","card":11,"emoji":"0x1F31E","level":4,"state":{"permModSources":{}},"lineup":["f0bcf4bb-74b3-412e-a54c-04c12ad28ecb","3db02423-92af-485f-b30f-78256721dcc6","fa477c92-39b6-4a52-b065-40af2f29840a","b5d4aaf7-1f89-476c-bee4-622cffdedbbc","df4da81a-917b-434f-b309-f00423ee4967","6644d767-ab15-4528-a4ce-ae1f8aadb65f"],"slogan":"Stare into the Sun...","shadows":["4562ac1f-026c-472c-b4e9-ee6ff800d701","e3c06405-0564-47ce-bbbd-552bee4dd66f","5a6b0c6d-1cc8-4acb-991c-0ffe62f3d990","5f5b3b18-ebc5-4611-b8bf-61b8c6a3ba12","24ca976f-ddf2-4966-ba14-4fd2b9b20341","aca90625-5bde-4489-bc9f-a7996a7b9a30","5ddee49d-79f2-4eba-a3c9-174784322059","72ca8ed5-a8c7-4571-9745-80de5a49b2d1","3dd85c20-a251-4903-8a3b-1b96941c07b7","206bd649-4f5f-4707-ad85-92784be4eb95","20fd71e7-4fa0-4132-9f47-06a314ed539a","088884af-f38d-4914-9d67-b319287481b4","1f145436-b25d-49b9-a1e3-2d3c91626211","14bfad43-2638-41ec-8964-8351f22e9c4f","459f7700-521e-40da-9483-4d111119d659","4c53fcd8-df02-4a80-99c9-51710863bc7f","b69aa26f-71f7-4e17-bc36-49c875872cc1","3d3be7b8-1cbf-450d-8503-fce0daf46cbf","e0d64270-6f5a-4c10-9b22-f91f9d4f241c","f2bf707d-5c40-4364-90ba-8d9e8348fca2","7207e4cf-d302-4bf5-81d9-7d41f3a1440e","5576a2cb-3deb-4a35-a1ef-9e3cc2039563","ceb8f8cd-80b2-47f0-b43e-4d885fa48aa4","92497eb4-4cb7-43f7-a32e-60b969784144"],"stadium":"8e900d85-7d4c-47d0-a67f-6432144f0210","deceased":false,"eDensity":1550.3786763270684,"fullName":"Hellmouth Sunbeams","gameAttr":[],"location":"Hellmouth","nickname":"Sunbeams","permAttr":["BASE_INSTINCTS"],"rotation":["5703141c-25d9-46d0-b680-0cf9cfbf4777","333067fd-c2b4-4045-a9a4-e87a8d0332d0","f883269f-117e-45ec-bb1e-fa8dbcf40d3e","89f74891-2e25-4b5a-bd99-c95ba3f36aa0","126fb128-7c53-45b5-ac2b-5dbf9943d71b","80dff591-2393-448a-8d88-122bd424fa4c"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#fffbab","shameRuns":0,"shorthand":"SUN","winStreak":-1,"imPosition":[0.45607139737781377,0.19064185430276842],"teamSpirit":0,"totalShames":109,"rotationSlot":0,"seasonShames":0,"championships":1,"totalShamings":95,"seasonShamings":0,"secondaryColor":"#fffbab","tournamentWins":0,"underchampionships":1},{"id":"f29d6e60-8fce-4ac6-8bc2-b5e3cabc5696","card":-1,"emoji":"0x1F50D","level":null,"state":{},"lineup":["d1a198d6-b05a-47cf-ab8e-39a6fa1ed831"],"slogan":"Drink In the Darkness","shadows":[],"stadium":null,"deceased":false,"eDensity":0,"fullName":"BC Noir","gameAttr":[],"location":"Black","nickname":"BC Noir","permAttr":[],"rotation":["fedbceb8-e2aa-4868-ac35-74cd0445893f"],"seasAttr":[],"weekAttr":[],"evolution":0,"mainColor":"#231709","shameRuns":0,"shorthand":"BCN","winStreak":0,"imPosition":[0,0],"teamSpirit":0,"totalShames":1,"rotationSlot":3,"seasonShames":1,"championships":0,"totalShamings":0,"seasonShamings":0,"secondaryColor":"#7f6f5c","tournamentWins":0,"underchampionships":0}]
  }
}
```

## databaseCommunityChestProgress
Gets how far along this instance is to having another community chest drop. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseCommunityChestProgress.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseCommunityChestProgress.type = "live"
Returns data from Blaseball, time agnostic.

### databaseCommunityChestProgress.type = "static"
Returns a static set of data, parsed from `databaseCommunityChestProgress.data`.

Example:
```json
{
  "databaseCommunityChestProgress": {
    "type": "static",
    "data": {
      "runs": "2741.20",
      "progress": "0.91",
      "chestsUnlocked": 22
    }
  }
}
```

## databaseBonusResults
Gets blessing data for this instance's elections. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseBonusResults.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseBonusResults.type = "live"
Returns data from Blaseball, time agnostic.

### databaseBonusResults.type = "static"
Returns a static set of data, parsed from `databaseBonusResults.data`.

Example:
```json
{
  "databaseBonusResults": {
    "type": "static",
    "data": [
      {
        "id": "00d60061-cb5e-4383-a786-c312fb8bb1e3",
        "teamId": "bfd38797-8404-4b38-8b82-341da28b1f83",
        "bonusId": "random_players_alttrust_lineup_three",
        "teamVotes": 3578,
        "bonusTitle": "Lineup Alternate Trust",
        "totalVotes": 57504,
        "description": "Velasquez Alstott's Alternate is called.\nVelasquez Alstott's Alternate arrived with Negative.\nJordan Hildebert's Alternate is called.\nJordan Hildebert's Alternate arrived with Negative.\nTad Seeth's Alternate is called.\nTad Seeth feels renewed.\nTad Seeth's Alternate arrived with Negative.",
        "highestTeam": "8d87c468-699a-47a8-b40d-cfb73a5660ad",
        "highestTeamVotes": 39483
      }
    ]
  }
}
```

## databaseDecreeResults
Gets decree results for this instance's elections. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseDecreeResults.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseDecreeResults.type = "live"
Returns data from Blaseball, time agnostic.

### databaseDecreeResults.type = "static"
Returns a static set of data, parsed from `databaseDecreeResults.data`.

Example:
```json
{
  "databaseDecreeResults": {
    "type": "static",
    "data": [
      {
        "id": "5adea39c-c013-48c0-b0fe-fc29539a2921",
        "decreeId": "trust_fall",
        "totalVotes": 5879869,
        "decreeTitle": "Trust Fall",
        "description": "STARS FALL\nASCENDING\nBELOW ZERO\nLEAPS OF FAITH\nTURNING TABLES\nTURN THE TABLES, THE GYM\nTURN THE TABLES, THE POCKET\nTURN THE TABLES, THE FIRE HOUSE\nTURN THE TABLES, THE AMPITHEATER\nTURN THE TABLES, THE BUCKET\nTURN THE TABLES, THE BUBBLE\nTURN THE TABLES, LA TAQUERÍA\nTURN THE TABLES, WORLDWIDE FIELD\nTURN THE TABLES, THE GARDEN\nTURN THE TABLES, THE SOLARIUM\nTURN THE TABLES, REDACTED\nTURN THE TABLES, THE WORMHOLE\nTURN THE TABLES, BATTIN' ISLAND\nTURN THE TABLES, THE STEAKHOUSE\nTURN THE TABLES, THE POLYHEDRON\nTURN THE TABLES, THE OVEN\nTURN THE TABLES, THE BIG GARAGE\nTURN THE TABLES, THE PILLARS\nTURN THE TABLES, THE COOKOUT\nTURN THE TABLES, THE GLEEK\nTURN THE TABLES, THE CHOUX\nTURN THE TABLES, YELLOWSTONE\nTURN THE TABLES, THE MEADOW\nTURN THE TABLES, THE CRABITAT"
      }
    ]
  }
}
```

## databaseEventResults
Gets tidings for this instance's election. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseEventResults.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseEventResults.type = "live"
Returns data from Blaseball, time agnostic.

### databaseEventResults.type = "static"
Returns a static set of data, parsed from `databaseEventResults.data`.

Example:
```json
{
  "databaseEventResults": {
    "type": "static",
    "data": [
      {
        "id": "98dbd7a2-4d76-4284-908b-818887c28dbb",
        "msg": "INVESTIGATION COMPLETE"
      },
      {
        "id": "0f15d631-e775-4589-83bf-e0228bc5f8f9",
        "msg": "Uncle Plasma was pulled from the ILB."
      },
      {
        "id": "e58d7b59-58a8-462b-a6f7-980b5e1a939f",
        "msg": "Liquid Friend was pulled from the ILB."
      },
      {
        "id": "4deafe68-290e-45ba-869f-aaba7c6c2386",
        "msg": "Uncle Plasma completed their Undercover assignment."
      },
      {
        "id": "9d86ba4c-5f10-4d4c-995a-3336657703a6",
        "msg": "Liquid Friend completed their Undercover assignment."
      },
      {
        "id": "3691fc10-7e9a-41ae-a18f-2dfe2318992d",
        "msg": "Uncle Plasma was returned to the Vault."
      },
      {
        "id": "f98fdf79-81ab-4392-bb6b-3893669f1e13",
        "msg": "Liquid Friend was returned to the Vault."
      },
      {
        "id": "52e1914f-fffc-4d14-b1e6-b3f2522661d7",
        "msg": "Liquid Friend was Unscrambled."
      },
      {
        "id": "736e1ebd-aac1-4ce1-80dd-38340362ecf8",
        "msg": "Liquid Friend was Taken."
      },
      {
        "id": "3daca946-0e99-4468-93d3-5a4b926abe2d",
        "msg": "Liquid Friend's Soul was Decocted by the Baristas."
      },
      {
        "id": "cdbd4145-5731-4c2b-9a2c-44b224afba5d",
        "msg": "Liquid Friend's Soul was Placed on the Bar."
      },
      {
        "id": "1ad7d58f-d4d3-473c-8e98-ac9589496010",
        "msg": "Liquid Friend's Soul was Served."
      },
      {
        "id": "2d6b67a1-e229-4868-93a3-bfcc7b09299b",
        "msg": "Uncle Plasma and Liquid Friend are stronger together."
      },
      {
        "id": "d0785548-601e-4071-8981-1d2f7055897f",
        "msg": "Liquid Friend and Uncle Plasma are stronger together."
      }
    ]
  }
}
```

## databaseFeedByPhase
Returns a list of feed events from the corresponding phase

Defaults to using [Upnuts](https://github.com/UnderMybrella/whats-upnut).

When it's a string, must be one of "upnuts", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseFeedByPhase.type = "upnuts"
Returns data from Upnuts for the corresponding phase, using only live Blaseball data for the instance's clock.

### databaseFeedByPhase.type = "live"
Returns data from Blaseball for the corresponding phase, time agnostic.

### databaseFeedByPhase.type = "static"
Returns a static set of data, parsed from `databaseFeedByPhase.data`.

Example:
```json
{
  "databaseFeedGlobal": {
    "type": "static",
    "data": [
      {
        "id": "86d32bb7-ccee-4202-8f37-289846e38057",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-03-08T02:41:46Z",
        "season": 11,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 0,
        "category": 4,
        "description": "hope its ok",
        "nuts": 143,
        "metadata": {
          "being": 1,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      },
      {
        "id": "37b801b0-134a-4d40-8120-822a2f2a637d",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-03-08T02:41:26Z",
        "season": 11,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 0,
        "category": 4,
        "description": "sorry",
        "nuts": 140,
        "metadata": {
          "being": 1,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      },
      {
        "id": "eb6efbc6-a0b6-4084-962c-00b45b194bc8",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-03-08T02:40:31Z",
        "season": 11,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 0,
        "category": 4,
        "description": "pretzels got wet",
        "nuts": 407,
        "metadata": {
          "being": 1,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      },
      {
        "id": "05988884-8b29-439a-9826-592283563167",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-03-08T02:39:08Z",
        "season": 11,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 0,
        "category": 4,
        "description": "new snacks",
        "nuts": 151,
        "metadata": {
          "being": 1,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      },
      {
        "id": "622607f8-3ebe-49c1-8613-a7653f9fe51b",
        "playerTags": [],
        "teamTags": [],
        "gameTags": [],
        "created": "2021-03-08T02:36:15Z",
        "season": 11,
        "tournament": -1,
        "type": 29,
        "day": 115,
        "phase": 0,
        "category": 4,
        "description": "hey",
        "nuts": 157,
        "metadata": {
          "being": 1,
          "_upnuts_hrefs": [
            "https://www.blaseball.com/global"
          ],
          "scales": null
        }
      }
    ]
  }
}
```

## databaseGameById
Gets game details for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseGameById.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseGameById.type = "live"
Returns data from Blaseball, time agnostic.

### databaseGameById.type = "static"
Returns a static set of data, parsed from `databaseGameById.data`.

Example:
```json
{
  "databaseGameById": {
    "type": "static",
    "data": {"id":"d162b23a-9832-4e78-8d78-5d131393fd61","basesOccupied":[],"baseRunners":[],"baseRunnerNames":[],"outcomes":["New Megan Ito traded their nothing for Dunlap Figueroa's The Fifth Base!","Traitor Adalberto Tosser traded their Traitor Chunky Necklace for Sexton Wheerer's Travelling Noise-Cancelling Night Vision Sunglasses of Intelligence!","Trader New Megan Ito traded their The Fifth Base for Parker MacMillan's The Force Field!","SUN(SUN) SUPERNOVA"],"terminology":"b67e9bbb-1495-4e1b-b517-f1444b0a6c8b","lastUpdate":"","rules":"df2207cc-03a2-4f6f-9604-63421a4dd5e8","statsheet":"a9bf2a40-0701-4f67-a626-b9ab03f3c3b1","awayPitcher":"d0d7b8fe-bad8-481f-978e-cb659304ed49","awayPitcherName":"Adalberto Tosser","awayBatter":null,"awayBatterName":"","awayTeam":"280c587b-e8f6-4a7e-a8ce-fd2fa2fa3e70","awayTeamName":"Rising Stars","awayTeamNickname":"Rising Stars","awayTeamColor":"#0c2357","awayTeamEmoji":"0x1F4AB","awayOdds":0,"awayStrikes":3,"awayScore":7,"awayTeamBatterCount":101,"homePitcher":"5eac7fd9-0d19-4bf4-a013-994acc0c40c0","homePitcherName":"Sutton Bishop","homeBatter":"864b3be8-e836-426e-ae56-20345b41d03d","homeBatterName":"Goodwin Morin","homeTeam":"698cfc6d-e95e-4391-b754-b87337e4d2a9","homeTeamName":"Vault Legends","homeTeamNickname":"Legends","homeTeamColor":"#c5c5c5","homeTeamEmoji":"0x1F3C5","homeOdds":0,"homeStrikes":3,"homeScore":5,"homeTeamBatterCount":122,"season":22,"isPostseason":false,"day":116,"phase":7,"gameComplete":true,"finalized":true,"gameStart":true,"halfInningOuts":0,"halfInningScore":0,"inning":28,"topOfInning":false,"atBatBalls":0,"atBatStrikes":2,"seriesIndex":0,"seriesLength":1,"shame":false,"weather":26,"baserunnerCount":0,"homeBases":4,"awayBases":4,"repeatCount":0,"awayTeamSecondaryColor":"#41b9ff","homeTeamSecondaryColor":"#fff8c6","homeBalls":4,"awayBalls":4,"homeOuts":3,"awayOuts":3,"playCount":1102,"tournament":-1,"baseRunnerMods":[],"homePitcherMod":"TRIPLE_THREAT","homeBatterMod":"","awayPitcherMod":"","awayBatterMod":"","scoreUpdate":"","scoreLedger":"","stadiumId":"73e2e5f9-6689-4e98-b061-c843a619d671","secretBaserunner":null,"topInningScore":0,"bottomInningScore":0,"newInningPhase":-1,"gameStartPhase":75,"isTitleMatch":false,"queuedEvents":[],"state":{"game_cancelled":true,"ego_player_data":[{"id":"0eddd056-9d72-4804-bd60-53144b785d5c","team":"b63be8c2-576a-4d6e-8daf-814f8bcea96f","location":1,"hallPlayer":false},{"id":"7aeb8e0b-f6fb-4a9e-bba2-335dada5f0a3","team":"c73b705c-40ad-4633-a6ed-d357ee2e2bcf","location":2,"hallPlayer":false},{"id":"a253facf-a54a-493e-b398-cf6f0d288990","hallPlayer":false},{"id":"ef9f8b95-9e73-49cd-be54-60f84858a285","team":"a37f9158-7f82-46bc-908c-c9e2dda7c33b","location":0,"hallPlayer":false},{"id":"836e9395-3f83-4d42-ba11-a12c08ceb78b","hallPlayer":false},{"id":"d0d7b8fe-bad8-481f-978e-cb659304ed49","team":"3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","location":0,"hallPlayer":false},{"id":"f2a27a7e-bf04-4d31-86f5-16bfa3addbe7","team":"adc5b394-8f76-416d-9ce9-813706877b84","location":1,"hallPlayer":false},{"id":"34267632-8c32-4a8b-b5e6-ce1568bb0639","hallPlayer":false},{"id":"defbc540-a36d-460b-afd8-07da2375ee63","team":"105bc3ff-1320-4e37-8ef0-8d595cb95dd0","location":1,"hallPlayer":false},{"id":"11de4da3-8208-43ff-a1ff-0b3480a0fbf1","team":"979aee4a-6d80-4863-bf1c-ee1a78e06024","location":1,"hallPlayer":false},{"id":"9e724d9a-92a0-436e-bde1-da0b2af85d8f","team":"747b8e4a-7e50-4638-a973-ea7950a3e739","location":0,"hallPlayer":false},{"id":"b082ca6e-eb11-4eab-8d6a-30f8be522ec4","team":"adc5b394-8f76-416d-9ce9-813706877b84","location":2,"hallPlayer":false},{"id":"b643a520-af38-42e3-8f7b-f660e52facc9","team":"46358869-dce9-4a01-bfba-ac24fc56f57e","location":0,"hallPlayer":false},{"id":"90c2cec7-0ed5-426a-9de8-754f34d59b39","team":"b72f3061-f573-40d7-832a-5ad475bd7909","location":0,"hallPlayer":false},{"id":"5eac7fd9-0d19-4bf4-a013-994acc0c40c0","hallPlayer":false},{"id":"44a22541-7704-403c-b637-3362a76943cb","hallPlayer":false},{"id":"4e6ad1a1-7c71-49de-8bd5-c286712faf9e","hallPlayer":false},{"id":"04e14d7b-5021-4250-a3cd-932ba8e0a889","team":"3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","location":0,"hallPlayer":false},{"id":"405dfadf-d435-4307-82f6-8eba2287e87a","team":"23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","location":0,"hallPlayer":false},{"id":"89f74891-2e25-4b5a-bd99-c95ba3f36aa0","team":"f02aeae2-5e6a-4098-9842-02d2273f25c7","location":0,"hallPlayer":false},{"id":"0bb35615-63f2-4492-80ec-b6b322dc5450","team":"878c1bf6-0d21-4659-bfee-916c8314d69c","location":0,"hallPlayer":false},{"id":"98a60f2f-cf34-4fcb-89aa-2d61fb5fba60","hallPlayer":false},{"id":"62ae6aa9-e346-4faa-b07c-1f7623580015","hallPlayer":false},{"id":"88cd6efa-dbf2-4309-aabe-ec1d6f21f98a","hallPlayer":false},{"id":"b5204124-6f90-46f6-bccc-b1ac11056cca","hallPlayer":false},{"id":"12c4368d-478b-42be-b6d3-fa2e9b230f82","team":"979aee4a-6d80-4863-bf1c-ee1a78e06024","location":0,"hallPlayer":false},{"id":"7a75d626-d4fd-474f-a862-473138d8c376","team":"36569151-a2fb-43c1-9df7-2df512424c82","location":0,"hallPlayer":false},{"id":"9a031b9a-16f8-4165-a468-5d0e28a81151","team":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","location":2,"hallPlayer":false},{"id":"a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21","team":"9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","location":0,"hallPlayer":false},{"id":"57a19a22-f2cd-4e59-aa84-15cb0af30ba3","team":"bb4a9de5-c924-4923-a0cb-9d1445f1ee5d","location":0,"hallPlayer":false},{"id":"083d09d4-7ed3-4100-b021-8fbe30dd43e8","team":"b024e975-1c4a-4575-8936-a3754a08806a","location":0,"hallPlayer":false},{"id":"4f615ee3-4615-4033-972f-79200f9db6e3","hallPlayer":false},{"id":"9820f2c5-f9da-4a07-b610-c2dd7bee2ef6","team":"878c1bf6-0d21-4659-bfee-916c8314d69c","location":2,"hallPlayer":false},{"id":"f9fe0130-4741-4103-af9c-86d85724ce54","hallPlayer":false},{"id":"0148c1b0-3b25-4ae1-a7ce-6b1c4f289747","hallPlayer":false},{"id":"678170e4-0688-436d-a02d-c0467f9af8c0","team":"a37f9158-7f82-46bc-908c-c9e2dda7c33b","location":0,"hallPlayer":false},{"id":"bf6a24d1-4e89-4790-a4ba-eeb2870cbf6f","team":"878c1bf6-0d21-4659-bfee-916c8314d69c","location":0,"hallPlayer":false},{"id":"f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1","hallPlayer":false},{"id":"63df8701-1871-4987-87d7-b55d4f1df2e9","team":"ca3f1c8c-c025-4d8e-8eef-5be6accbeb16","location":1,"hallPlayer":false},{"id":"0813d43c-b938-49da-acef-65b73f27b099","team":"bb4a9de5-c924-4923-a0cb-9d1445f1ee5d","location":0,"hallPlayer":false},{"id":"bc29afc1-c954-4def-978b-a59ae5def3c3","team":"d9f89a8a-c563-493e-9d64-78e4f9a55d4a","location":0,"hallPlayer":false},{"id":"90768354-957e-4b4c-bb6d-eab6bbda0ba3","team":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","location":0,"hallPlayer":false},{"id":"338694b7-6256-4724-86b6-3884299a5d9e","team":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","location":0,"hallPlayer":false},{"id":"5c983667-6d14-4393-8f15-af904d8f90f8","team":"747b8e4a-7e50-4638-a973-ea7950a3e739","location":0,"hallPlayer":false},{"id":"3d293d6c-3a20-43ab-a895-2b7f1f28779f","team":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","location":0,"hallPlayer":false}]},"endPhase":5,"newHalfInningPhase":-1}
  }
}
```

## databaseGetPreviousChamp
Gets previous reigning over/under champion. Must either be a string, or an object.

Note that before 2021-07-19T14:50:00.000Z, this endpoint only returns one team, normally the 'last' update to the data. After, it returns an object with the over/under champion.

Defaults to returning data from a query lookup.

When it's a string, must be "query_lookup", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseGetPreviousChamp.type = "query_lookup"
Returns data from an internal query lookup (holdover until Chronicler supports this endpoint)

### databaseGetPreviousChamp.type = "live"
Returns data from Blaseball, time agnostic.

### databaseGetPreviousChamp.type = "static"
Returns a static set of data, parsed from `databaseGetPreviousChamp.data`.

Example:
```json
{
  "databaseGetPreviousChamp": {
    "type": "static",
    "data": {"over":{"id":"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","lineup":["238f8c50-311f-4486-a734-e1a17606af4d","338694b7-6256-4724-86b6-3884299a5d9e","ceb5606d-ea3f-4471-9ca7-3d2e71a50dde"],"rotation":["e6502bc7-5b76-4939-9fb8-132057390b30","3d293d6c-3a20-43ab-a895-2b7f1f28779f","ce7d33b7-1bba-48d6-a1d1-cd0e29231be8","32f3f7f8-e178-4d79-ac58-7a73abaf60aa","28162da7-eafa-4eb1-8bc1-5a625f03ae57","8f11ad58-e0b9-465c-9442-f46991274557","36786f44-9066-4028-98d9-4fa84465ab9e","a691f2ba-9b69-41f8-892c-1acd42c336e4","27faa5a7-d3a8-4d2d-8e62-47cfeba74ff0","90768354-957e-4b4c-bb6d-eab6bbda0ba3","f741dc01-2bae-4459-bfc0-f97536193eea"],"seasAttr":[],"permAttr":["HIGH_PRESSURE","H20","MODERATION"],"fullName":"Canada Moist Talkers","location":"Canada","mainColor":"#f5feff","nickname":"Moist Talkers","secondaryColor":"#f5feff","shorthand":"CAN","emoji":"0x1F5E3","slogan":"Talk Spit, Get Hits","shameRuns":0,"totalShames":109,"totalShamings":114,"seasonShames":0,"seasonShamings":0,"championships":3,"weekAttr":[],"gameAttr":[],"rotationSlot":0,"teamSpirit":0,"card":4,"tournamentWins":0,"stadium":"f71fd587-d20c-461c-88a4-1634a2df3f43","imPosition":[-1,0.19268842442149015],"eDensity":1549.6684102053357,"state":{"permModSources":{}},"evolution":0,"winStreak":1,"level":4,"shadows":["6524e9e0-828a-46c4-935d-0ee2edeb7e9a","51cba429-13e8-487e-9568-847b7b8b9ac5","24cb35c1-c24c-45ca-ac0b-f99a2e650d89","9ba361a1-16d5-4f30-b590-fc4fc2fb53d2","c90eafdf-4244-41e0-8a66-adf31d700f02","c09e66b3-5847-4cbb-aeba-2b70e7bafedb","9a031b9a-16f8-4165-a468-5d0e28a81151","750c7d44-757a-430c-9fa3-d4ca22111933","9fc3c6ae-5ed5-49ce-a75b-257ec1022a15","a1b55c5f-6d01-4ca1-976a-5cdfe18d199c","64f59d5f-8740-4ebf-91bd-d7697b542a9f","adcbc290-81c5-4da9-b4da-2e594b080710","3a96d76a-c508-45a0-94a0-8f64cd6beeb4","c31d874c-1b4d-40f2-a1b3-42542e934047","04a7c002-fcd1-4948-bb35-fd0ba383406c","c57222fd-df55-464c-a44e-b15443e61b70","c3b1b4e5-4b88-4245-b2b1-ae3ade57349e","aa612fa0-5d16-49fa-b1f6-00497c0448ad","3954bdfa-931f-4787-b9ac-f44b72fe09d7","0f6e8b92-304d-4720-b650-9af5150fa9bd","a0e7445f-2b93-4ca7-a69d-ff32593700ee","187d4a44-4deb-41f1-99d2-b013419dda69"],"underchampionships":0,"deceased":false},"under":{"id":"7966eb04-efcc-499b-8f03-d13916330531","lineup":["8c8cc584-199b-4b76-b2cd-eaa9a74965e5","17392be2-7344-48a0-b4db-8a040a7fb532","44c92d97-bb39-469d-a13b-f2dd9ae644d1","94f30f21-f889-4a2e-9b94-818475bb1ca0","070758a0-092a-4a2c-8a16-253c835887cb","2ffbaec8-a646-4435-a17a-3a0a2b5f3e16","ac57cf28-556f-47af-9154-6bcea2ace9fc","9be56060-3b01-47aa-a090-d072ef109fbf"],"rotation":["8adb084b-19fe-4295-bcd2-f92afdb62bd7","aa6c2662-75f8-4506-aa06-9a0993313216","09f2787a-3352-41a6-8810-d80e97b253b5","24f6829e-7bb4-4e1e-8b59-a07514657e72"],"seasAttr":[],"permAttr":["O_NO"],"fullName":"Yellowstone Magic","location":"Yellowstone","mainColor":"#bf0043","nickname":"Magic","secondaryColor":"#f60f63","shorthand":"YELL","emoji":"0x2728","slogan":"As Above, So Below","shameRuns":0,"totalShames":107,"totalShamings":93,"seasonShames":0,"seasonShamings":0,"championships":0,"weekAttr":[],"gameAttr":[],"rotationSlot":0,"teamSpirit":0,"card":0,"tournamentWins":0,"stadium":"2a1a52b3-9759-44aa-ba49-b1437396d895","imPosition":[0.33439374154760393,0.6084452638794811],"eDensity":394.97561569148655,"state":{"permModSources":{}},"evolution":0,"winStreak":-1,"level":1,"shadows":["945974c5-17d9-43e7-92f6-ba49064bbc59","c6146c45-3d9b-4749-9f03-d4faae61e2c3","03b80a57-77ea-4913-9be4-7a85c3594745","1af239ae-7e12-42be-9120-feff90453c85","7b779947-46ef-4b77-be37-962d60875647","f617c1ef-d676-4119-8359-a5c3ac3bef51","11a2a8a7-8c92-447b-9448-2ec5ac93bf65","450e6483-d116-41d8-933b-1b541d5f0026","ac69dba3-6225-4afd-ab4b-23fc78f730fb","1ded0384-d290-4ea1-a72b-4f9d220cbe37","82733eb4-103d-4be1-843e-6eb6df35ecd7","db53211c-f841-4f33-accf-0c3e167889a0","b77dffaa-e0f5-408f-b9f2-1894ed26e744","3205f4ff-7050-470b-8970-8d7ea56564bc","e4f1f358-ee1f-4466-863e-f329766279d0","5954374a-8b46-4d25-8524-2a6ad45328b5","51dab868-820b-4969-8bba-bde0be8090d7","a4133ba6-0299-4953-8148-e64584b433a6"],"underchampionships":1,"deceased":false}}
  }
}
```

## databaseGiftProgress
Gets gift progress for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseGiftProgress.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseGiftProgress.type = "live"
Returns data from Blaseball, time agnostic.

### databaseGiftProgress.type = "static"
Returns a static set of data, parsed from `databaseGiftProgress.data`.

Example:
```json
{
  "databaseGiftProgress": {
    "type": "static",
    "data": {"teamProgress":{"105bc3ff-1320-4e37-8ef0-8d595cb95dd0":{"total":3,"toNext":0.02},"23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7":{"total":3,"toNext":0.01},"36569151-a2fb-43c1-9df7-2df512424c82":{"total":3,"toNext":0.01},"3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e":{"total":2,"toNext":0.3},"46358869-dce9-4a01-bfba-ac24fc56f57e":{"total":2,"toNext":0.46},"57ec08cc-0411-4643-b304-0e80dbc15ac7":{"total":2,"toNext":0.02},"747b8e4a-7e50-4638-a973-ea7950a3e739":{"total":2,"toNext":0.05},"7966eb04-efcc-499b-8f03-d13916330531":{"total":2,"toNext":0.06},"878c1bf6-0d21-4659-bfee-916c8314d69c":{"total":2,"toNext":0.05},"8d87c468-699a-47a8-b40d-cfb73a5660ad":{"total":2,"toNext":0.17},"979aee4a-6d80-4863-bf1c-ee1a78e06024":{"total":2,"toNext":0.19},"9debc64f-74b7-4ae1-a4d6-fce0144b6ea5":{"total":2,"toNext":0.33},"a37f9158-7f82-46bc-908c-c9e2dda7c33b":{"total":3,"toNext":0.04},"adc5b394-8f76-416d-9ce9-813706877b84":{"total":3,"toNext":0.01},"b024e975-1c4a-4575-8936-a3754a08806a":{"total":2,"toNext":0.03},"b63be8c2-576a-4d6e-8daf-814f8bcea96f":{"total":2,"toNext":0.02},"b72f3061-f573-40d7-832a-5ad475bd7909":{"total":2,"toNext":0.08},"bb4a9de5-c924-4923-a0cb-9d1445f1ee5d":{"total":2,"toNext":0.06},"bfd38797-8404-4b38-8b82-341da28b1f83":{"total":2,"toNext":0.02},"c73b705c-40ad-4633-a6ed-d357ee2e2bcf":{"total":2,"toNext":0.13},"ca3f1c8c-c025-4d8e-8eef-5be6accbeb16":{"total":3,"toNext":0.05},"d9f89a8a-c563-493e-9d64-78e4f9a55d4a":{"total":3,"toNext":0.01},"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff":{"total":2,"toNext":0.38},"f02aeae2-5e6a-4098-9842-02d2273f25c7":{"total":2,"toNext":0.21}},"teamWishLists":{"00245773-6f25-43b1-a863-42b4068888f0":[{"bonus":"random_player_phoenix_quill","percent":1}],"105bc3ff-1320-4e37-8ef0-8d595cb95dd0":[{"bonus":"bargain_bin","percent":0.38},{"bonus":"edense_item_infusion","percent":0.38},{"bonus":"late_to_party_team","percent":0.08},{"bonus":"random_player_steel_chair","percent":0.07},{"bonus":"adense_item_infusion","percent":0.05},{"bonus":"ambitious_solo","percent":0.03},{"bonus":"random_player_squiddish_egg","percent":0}],"23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7":[{"bonus":"random_player_fliickerrriiing_item","percent":0.31},{"bonus":"late_to_party_team","percent":0.3},{"bonus":"random_player_steel_chair","percent":0.26},{"bonus":"soul_patches_10","percent":0.11},{"bonus":"random_player_night_vision_sunglasses","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0.01}],"36569151-a2fb-43c1-9df7-2df512424c82":[{"bonus":"late_to_party_team","percent":0.44},{"bonus":"ambitious_solo","percent":0.26},{"bonus":"adense_item_infusion","percent":0.19},{"bonus":"random_player_fliickerrriiing_item","percent":0.09},{"bonus":"edense_item_infusion","percent":0.01},{"bonus":"random_player_steel_chair","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0}],"3a094991-4cbc-4786-b74c-688876d243f4":[{"bonus":"random_player_phoenix_quill","percent":1}],"3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e":[{"bonus":"random_player_squiddish_egg","percent":0.44},{"bonus":"early_to_party_team","percent":0.25},{"bonus":"edense_item_infusion","percent":0.22},{"bonus":"adense_item_infusion","percent":0.03},{"bonus":"random_player_night_vision_sunglasses","percent":0.03},{"bonus":"ambitious_solo","percent":0.01},{"bonus":"bargain_bin","percent":0.01},{"bonus":"soul_patches_10","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0},{"bonus":"unambitious_solo","percent":0}],"46358869-dce9-4a01-bfba-ac24fc56f57e":[{"bonus":"early_to_party_team","percent":0.41},{"bonus":"unambitious_solo","percent":0.34},{"bonus":"random_player_steel_chair","percent":0.19},{"bonus":"late_to_party_team","percent":0.05},{"bonus":"random_player_squiddish_egg","percent":0.01},{"bonus":"bargain_bin","percent":0},{"bonus":"random_player_night_vision_sunglasses","percent":0},{"bonus":"random_player_phoenix_quill","percent":0}],"57ec08cc-0411-4643-b304-0e80dbc15ac7":[{"bonus":"soul_patches_10","percent":0.65},{"bonus":"random_player_steel_chair","percent":0.18},{"bonus":"early_to_party_team","percent":0.14},{"bonus":"adense_item_infusion","percent":0.02},{"bonus":"random_player_fliickerrriiing_item","percent":0},{"bonus":"random_player_night_vision_sunglasses","percent":0},{"bonus":"random_player_phoenix_quill","percent":0},{"bonus":"random_player_squiddish_egg","percent":0}],"71c621eb-85dc-4bd7-a690-0c68c0e6fb90":[{"bonus":"bargain_bin","percent":0.93},{"bonus":"random_player_fliickerrriiing_item","percent":0.03},{"bonus":"soul_patches_10","percent":0.03}],"747b8e4a-7e50-4638-a973-ea7950a3e739":[{"bonus":"early_to_party_team","percent":0.3},{"bonus":"random_player_steel_chair","percent":0.25},{"bonus":"unambitious_solo","percent":0.22},{"bonus":"random_player_trader_item","percent":0.16},{"bonus":"late_to_party_team","percent":0.04},{"bonus":"random_player_night_vision_sunglasses","percent":0.04},{"bonus":"random_player_phoenix_quill","percent":0}],"7966eb04-efcc-499b-8f03-d13916330531":[{"bonus":"early_to_party_team","percent":0.26},{"bonus":"adense_item_infusion","percent":0.23},{"bonus":"random_player_squiddish_egg","percent":0.19},{"bonus":"random_player_night_vision_sunglasses","percent":0.18},{"bonus":"random_player_steel_chair","percent":0.07},{"bonus":"random_player_phoenix_quill","percent":0.05},{"bonus":"bargain_bin","percent":0.02},{"bonus":"late_to_party_team","percent":0.01}],"878c1bf6-0d21-4659-bfee-916c8314d69c":[{"bonus":"random_player_night_vision_sunglasses","percent":0.47},{"bonus":"random_player_steel_chair","percent":0.27},{"bonus":"random_player_squiddish_egg","percent":0.2},{"bonus":"late_to_party_team","percent":0.03},{"bonus":"early_to_party_team","percent":0.02},{"bonus":"soul_patches_10","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0}],"8d87c468-699a-47a8-b40d-cfb73a5660ad":[{"bonus":"random_player_night_vision_sunglasses","percent":0.46},{"bonus":"random_player_squiddish_egg","percent":0.37},{"bonus":"random_player_steel_chair","percent":0.12},{"bonus":"late_to_party_team","percent":0.05},{"bonus":"early_to_party_team","percent":0.01}],"939db13f-79c9-41c5-9a15-b340b1bea875":[{"bonus":"bargain_bin","percent":0.33},{"bonus":"early_to_party_team","percent":0.33},{"bonus":"random_player_steel_chair","percent":0.33}],"979aee4a-6d80-4863-bf1c-ee1a78e06024":[{"bonus":"early_to_party_team","percent":0.4},{"bonus":"random_player_steel_chair","percent":0.3},{"bonus":"random_player_phoenix_quill","percent":0.23},{"bonus":"unambitious_solo","percent":0.04},{"bonus":"random_player_squiddish_egg","percent":0.03},{"bonus":"random_player_night_vision_sunglasses","percent":0}],"9debc64f-74b7-4ae1-a4d6-fce0144b6ea5":[{"bonus":"adense_item_infusion","percent":0.35},{"bonus":"late_to_party_team","percent":0.34},{"bonus":"early_to_party_team","percent":0.24},{"bonus":"random_player_night_vision_sunglasses","percent":0.04},{"bonus":"ambitious_solo","percent":0.03},{"bonus":"bargain_bin","percent":0}],"a37f9158-7f82-46bc-908c-c9e2dda7c33b":[{"bonus":"random_player_fliickerrriiing_item","percent":0.47},{"bonus":"late_to_party_team","percent":0.16},{"bonus":"random_player_steel_chair","percent":0.13},{"bonus":"random_player_phoenix_quill","percent":0.11},{"bonus":"early_to_party_team","percent":0.09},{"bonus":"random_player_night_vision_sunglasses","percent":0.02},{"bonus":"random_player_squiddish_egg","percent":0.02},{"bonus":"ambitious_solo","percent":0}],"adc5b394-8f76-416d-9ce9-813706877b84":[{"bonus":"early_to_party_team","percent":0.41},{"bonus":"random_player_steel_chair","percent":0.36},{"bonus":"random_player_squiddish_egg","percent":0.22}],"b024e975-1c4a-4575-8936-a3754a08806a":[{"bonus":"random_player_steel_chair","percent":0.39},{"bonus":"early_to_party_team","percent":0.38},{"bonus":"late_to_party_team","percent":0.09},{"bonus":"random_player_squiddish_egg","percent":0.08},{"bonus":"random_player_phoenix_quill","percent":0.05}],"b63be8c2-576a-4d6e-8daf-814f8bcea96f":[{"bonus":"late_to_party_team","percent":0.36},{"bonus":"soul_patches_10","percent":0.35},{"bonus":"random_player_steel_chair","percent":0.23},{"bonus":"early_to_party_team","percent":0.04},{"bonus":"ambitious_solo","percent":0.01},{"bonus":"random_player_night_vision_sunglasses","percent":0.01}],"b6b5df8f-5602-4883-b47d-07e77ed9d5af":[{"bonus":"random_player_steel_chair","percent":0.67},{"bonus":"random_player_fliickerrriiing_item","percent":0.33}],"b72f3061-f573-40d7-832a-5ad475bd7909":[{"bonus":"early_to_party_team","percent":0.55},{"bonus":"random_player_steel_chair","percent":0.24},{"bonus":"random_player_squiddish_egg","percent":0.1},{"bonus":"late_to_party_team","percent":0.08},{"bonus":"random_player_phoenix_quill","percent":0.01},{"bonus":"soul_patches_10","percent":0.01},{"bonus":"bargain_bin","percent":0}],"bb4a9de5-c924-4923-a0cb-9d1445f1ee5d":[{"bonus":"late_to_party_team","percent":0.5},{"bonus":"ambitious_solo","percent":0.29},{"bonus":"random_player_phoenix_quill","percent":0.09},{"bonus":"random_player_night_vision_sunglasses","percent":0.08},{"bonus":"random_player_steel_chair","percent":0.03},{"bonus":"random_player_squiddish_egg","percent":0.01},{"bonus":"soul_patches_10","percent":0}],"bfd38797-8404-4b38-8b82-341da28b1f83":[{"bonus":"random_player_steel_chair","percent":0.39},{"bonus":"late_to_party_team","percent":0.31},{"bonus":"random_player_squiddish_egg","percent":0.21},{"bonus":"random_player_night_vision_sunglasses","percent":0.05},{"bonus":"adense_item_infusion","percent":0.01},{"bonus":"random_player_fliickerrriiing_item","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0.01},{"bonus":"random_player_trader_item","percent":0.01},{"bonus":"bargain_bin","percent":0}],"c73b705c-40ad-4633-a6ed-d357ee2e2bcf":[{"bonus":"late_to_party_team","percent":0.37},{"bonus":"random_player_steel_chair","percent":0.3},{"bonus":"random_player_phoenix_quill","percent":0.19},{"bonus":"early_to_party_team","percent":0.07},{"bonus":"random_player_squiddish_egg","percent":0.07},{"bonus":"bargain_bin","percent":0},{"bonus":"random_player_fliickerrriiing_item","percent":0}],"ca3f1c8c-c025-4d8e-8eef-5be6accbeb16":[{"bonus":"late_to_party_team","percent":0.52},{"bonus":"random_player_steel_chair","percent":0.29},{"bonus":"ambitious_solo","percent":0.18},{"bonus":"random_player_night_vision_sunglasses","percent":0.01},{"bonus":"random_player_phoenix_quill","percent":0.01}],"d9f89a8a-c563-493e-9d64-78e4f9a55d4a":[{"bonus":"random_player_steel_chair","percent":0.28},{"bonus":"early_to_party_team","percent":0.24},{"bonus":"bargain_bin","percent":0.22},{"bonus":"edense_item_infusion","percent":0.19},{"bonus":"late_to_party_team","percent":0.05},{"bonus":"unambitious_solo","percent":0.01},{"bonus":"ambitious_solo","percent":0},{"bonus":"random_player_night_vision_sunglasses","percent":0},{"bonus":"soul_patches_10","percent":0}],"eb67ae5e-c4bf-46ca-bbbc-425cd34182ff":[{"bonus":"late_to_party_team","percent":0.3},{"bonus":"ambitious_solo","percent":0.29},{"bonus":"random_player_steel_chair","percent":0.18},{"bonus":"random_player_squiddish_egg","percent":0.12},{"bonus":"random_player_phoenix_quill","percent":0.11},{"bonus":"adense_item_infusion","percent":0},{"bonus":"bargain_bin","percent":0},{"bonus":"random_player_fliickerrriiing_item","percent":0},{"bonus":"random_player_night_vision_sunglasses","percent":0},{"bonus":"random_player_trader_item","percent":0}],"f02aeae2-5e6a-4098-9842-02d2273f25c7":[{"bonus":"ambitious_solo","percent":0.31},{"bonus":"late_to_party_team","percent":0.27},{"bonus":"random_player_steel_chair","percent":0.16},{"bonus":"random_player_phoenix_quill","percent":0.12},{"bonus":"random_player_squiddish_egg","percent":0.04},{"bonus":"bargain_bin","percent":0.03},{"bonus":"soul_patches_10","percent":0.03},{"bonus":"unambitious_solo","percent":0.03},{"bonus":"random_player_fliickerrriiing_item","percent":0.01},{"bonus":"adense_item_infusion","percent":0},{"bonus":"random_player_trader_item","percent":0}]}}
  }
}
```

## databaseGlobalEvents
Gets ticker messages displayed at the top of the instance page. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseGlobalEvents.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseGlobalEvents.type = "live"
Returns data from Blaseball, time agnostic.

### databaseGlobalEvents.type = "static"
Returns a static set of data, parsed from `databaseGlobalEvents.data`.

Example:
```json
{
  "databaseGlobalEvents": {
    "type": "static",
    "data": [
      {
        "id": "omg",
        "msg": "HOLY WHOA",
        "expire": null
      }
    ]
  }
}
```

## databaseItems
Gets items from the provided IDs for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseItems.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseItems.type = "live"
Returns data from Blaseball, time agnostic.

### databaseItems.type = "static"
Returns a static set of data, parsed from `databaseItems.data`.

Example:
```json
{
  "databaseItems": {
    "type": "static",
    "data": [
      {
        "id": "ed5a91e4-cfac-11eb-b8bc-0242ac130003",
        "name": "The Force Field",
        "forger": null,
        "forgerName": null,
        "prePrefix": null,
        "prefixes": [
          {
            "name": "Weird",
            "adjustments": [
              {
                "stat": 0,
                "type": 1,
                "value": 0.11303545672163
              },
              {
                "stat": 14,
                "type": 1,
                "value": 0.09853018273652352
              },
              {
                "stat": 13,
                "type": 1,
                "value": 0.20573602355163453
              },
              {
                "stat": 6,
                "type": 1,
                "value": 0.0561124729594477
              },
              {
                "stat": 8,
                "type": 1,
                "value": 0.16309883862682356
              }
            ]
          },
          {
            "name": "Metaphorical",
            "adjustments": [
              {
                "stat": 14,
                "type": 1,
                "value": 0.20661302540963317
              },
              {
                "stat": 23,
                "type": 1,
                "value": 0.32987749584299714
              },
              {
                "stat": 25,
                "type": 1,
                "value": 0.23846613342398926
              },
              {
                "stat": 11,
                "type": 1,
                "value": -0.16879078804601033
              }
            ]
          },
          {
            "name": "Cryogenic",
            "adjustments": [
              {
                "stat": 15,
                "type": 1,
                "value": 0.19776236406662423
              },
              {
                "stat": 17,
                "type": 1,
                "value": 0.24947840986592143
              },
              {
                "stat": 18,
                "type": 1,
                "value": 0.14749198972330058
              }
            ]
          },
          {
            "name": "Careful",
            "adjustments": [
              {
                "mod": "CAREFUL",
                "type": 0
              }
            ]
          }
        ],
        "postPrefix": {
          "name": "Force",
          "adjustments": [
            {
              "mod": "FORCE",
              "type": 0
            }
          ]
        },
        "root": {
          "name": "Field",
          "adjustments": [
            {
              "stat": 22,
              "type": 1,
              "value": 0.1803251121266278
            }
          ]
        },
        "suffix": {
          "name": "Containment",
          "adjustments": [
            {
              "mod": "CONTAINMENT",
              "type": 0
            }
          ]
        },
        "durability": -1,
        "health": 1,
        "baserunningRating": 0.0003732381020422615,
        "pitchingRating": 0.08610393849816389,
        "hittingRating": null,
        "defenseRating": 0.09465584705519436,
        "state": {}
      }
    ]
  }
}
```

## databaseOffseasonRecap
Gets **all** divisions for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseOffseasonRecap.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseOffseasonRecap.type = "live"
Returns data from Blaseball, time agnostic.

### databaseOffseasonRecap.type = "static"
Returns a static set of data, parsed from `databaseOffseasonRecap.data`.

Example:
```json
{
  "databaseOffseasonRecap": {
    "type": "static",
    "data": {"id":"adb70dc5-d959-471a-a326-5bb4ee9f4d17","bonusResults":["fa1e280e-f490-490f-b0d9-9bdd46235f1b","f7c41775-3481-4317-8f3e-524c6749091a","c05782ef-adfd-484f-bfbe-34ed8914133d","e792ba81-f11b-437e-a370-6a607040757d","60e8aa4c-a503-4f87-82c2-d003c1219f83","7d218b28-477f-43d2-be23-80dedbac3ee6","7fa5dcfe-e979-4a43-a114-14d539eb7d6e","fd344b65-a473-431e-9b06-80c39b0aa933","5cef2397-4fb1-4a06-933f-0acc08e564f8","3b28fc3c-82d7-49dd-a239-5b0481b7de24","4d769c15-0338-4eef-a13e-a3e7ea0f39ca","505fc463-a468-4825-8d34-6b81c3618be0","36003d65-0727-4381-bee2-a2dd124d6860","1f45b2b6-975e-457d-af70-092966799c56","c55315f3-a88c-459a-9016-5348138e1fce","119e74ba-967f-4dce-a5a0-bed324ce1768","9059cac7-bfcf-4d07-85fe-cd5acfd4d6fa","fe0738a6-97d9-4b83-bb0b-1874bfe66456","a24e6e88-5517-45ea-b706-3d97d2d1a41e","0f4be9fc-bb82-4f60-9872-4f426a2d00b2","29c771b5-d0a3-4941-849e-b595d8a5d61e","7d89c481-d779-4183-8786-53a21440fe07","4cd576b9-004f-4b6a-a0a0-f2404e8b3013","b86f5ae0-e4cd-43a1-b799-66a5d356ea14"],"decreeResults":["9f604e04-e6e0-497f-8d6b-06c9192baa4f","c513a34b-532b-4bae-9856-98a782764360","9143ef2e-ffdd-4c51-8766-7800e2946b01","65a57613-6d45-4130-926d-8d60f8004605","37a80fb4-433f-4020-9517-0623edb72a78","36ded2ef-4ec8-4b49-8b80-844e2f7e4b6f","a9a6042a-bdf2-401c-ac7b-5728f772c03c","78303e90-8f60-4bf4-a1e1-4df5013194af","456d75dd-5a5a-4ece-8f6c-b643dcae140f","f3df840d-596b-4725-8348-c0b6be43c2df","050434b3-169f-4041-9d3e-221c4869bd40","afb8b219-42c7-4444-9c34-3c083d0def88","229e8deb-b0fa-493b-b703-db174974327b","3003f8d8-a464-4826-a631-05856b195cbb","9472079a-e34c-4c3e-ad24-4bc5dc2c4549","1c1b498f-bfb3-4895-aa74-f86dbacd99b5","30e1ec72-9802-4771-8d36-e9329747a7ae","bf5544b4-3b75-4290-9dd1-6a942466aeac","cf73133e-080c-474d-85ee-a10fb02ce111","e54073d1-0627-4aeb-8bab-2a482ea4bd04","5e99f7b7-9577-4a17-bf01-afef1172ef8d","a656a63e-29e5-48a4-a51d-4d98507b52a7","f0cd1b85-1ef1-4d6f-9630-94d284ff557f","887aa5bf-a268-462c-8a20-b923d00f5350"],"name":"The Season 23 Election","season":22,"totalBonusVotes":59713304,"totalDecreeVotes":14278464,"voteCount":73991768,"eventResults":["e7d44c50-2aca-4034-b2cb-9eb5ecf56ec2","a0323662-7565-4c06-8d0c-a9750a28b7d1","223fcb5d-e450-4f6f-bcbd-e7ca76fd1f30","9c62356c-f986-4f1e-9044-a2b34aac7122","0d84b7c4-0f3c-4b6d-aa93-28c9c87a22d2","d193446f-6af7-468e-bb52-505f3d4bee5a","0641c39f-e92f-44be-ac42-0028965b9327","f23a35b5-39d4-46d5-b407-6838cddd7db9","394063c8-80e4-443e-800c-77c384b27029","ec653699-4eca-4a62-9e2b-270679cf8084","f98a5c3f-66d5-4bd6-b089-fa0bebc9a56d","eea3c8b8-3608-498f-bc08-3eee120f2010","2e24a116-4e4d-4a3c-ae89-7793bdfd30ac","344a38d4-b198-4fb5-ae8d-30144f397e1e","5285c90f-fd0a-4abe-8795-aa23c9e60af8","4e19f204-a5ba-4dbd-83f2-57f0bf9a0c9d","3b4a42c4-6cfe-43ca-ae64-754383a0462c","5a478476-0f4a-4312-ab71-73bb5326eb75","5137bc4a-28fc-4cca-9047-5006af4700fe","fae6b9c9-b661-4fff-b00c-24f1ec3d7979","facc08ef-f69a-4fd7-8a4d-e858b8f1a7f7","bbd1c64e-b349-4ba4-acb8-c8597bb5f706","992b7a6e-725f-4cd3-9b41-2b4198cf51d3","17579f3e-85d6-4bba-8dc4-1acf35504ac8","c107f00f-1c96-4d85-b93c-4fd541cae4e8","d77e2071-ccae-416b-bff2-e8c04e6b7bd9","126fd63b-cb0d-4da7-9b92-593b68958a4c","1812ef51-e7b5-44a4-b93e-119d0206d2c9","56607e0e-bd9f-43c3-9e8b-9eee0e2781e1","367bd95b-a72d-4b12-a5e9-a64113aba176","efb2f7ed-4a3e-4b3d-b005-7aa9432eaa8a","e101e96d-48aa-4465-a600-667a3d6b7795","f2e289cf-9647-4d2d-a229-c0fe9d8262f9","141ca61d-dd3f-494e-b765-d5a449af9a44","1fc73dc5-5ca8-460f-8c82-da72a4c2d322","c942af89-4139-4da6-aaf5-9dbbe26b2565","3f918e24-3a1b-401a-987c-35df487adedb","0d2b6120-ec7b-4262-a755-f40e709ef6b9","6a3798a1-b9cd-45d7-a344-21d71b4c30d8","63f38ab7-d7f1-4742-861e-e0b15ddb6b35","91a12974-e39e-4f2a-9462-5d5b56b265a7","ea819a15-8390-49a5-9d15-9dfa5257f9a8","f9b80064-b96f-4938-af76-4b1e32496404","3ce3bb7d-498f-48a5-8ba8-64684e3d4307","e12848c8-36c1-4f3b-83fb-9d8b3f69eaab","7cf9e29c-d670-46cd-8486-7f0f7d9c0138","52973d76-a2f3-45e2-bfa2-6ec7d1cdc66c","9a74a579-c9f3-4714-a4c0-a5f01f4657cb","ce04bea4-bfcd-4911-af13-4b0df5cc5c5b","36141a85-785e-49a2-8046-2322d5a421de","2e7d3ea6-7cf4-4318-825e-f0bc58abd0da","6c8d3e8c-c4aa-4346-b192-82f7e7e4e7b0","7edc56dc-2178-4496-9fb6-6f5a780ce8df","16a340f8-7f48-47fa-b1ff-d37645957516","f4ee3140-ead7-4c55-9fcb-2f6b130529f8"],"willResults":[],"totalWillVotes":0}
  }
}
```

## databaseOffseasonSetup
Gets election and giftshop data for this season. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseOffseasonSetup.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseOffseasonSetup.type = "live"
Returns data from Blaseball, time agnostic.

### databaseOffseasonSetup.type = "static"
Returns a static set of data, parsed from `databaseOffseasonSetup.data`.

Example:
```json
{
  "databaseOffseasonSetup": {
    "type": "static",
    "data": {"decrees":[{"id":"team_favor_roamless","type":92,"title":"Team Roamless Crate","description":"Players cannot Roam to your Team.","metadata":{"eventDescription":"The {tnn} became Roamless.","mod":"ROAMLESS","modType":0}},{"id":"team_favor_squiddish","type":92,"title":"Team Squiddish Crate","description":"Your Team will become Squiddish.","metadata":{"eventDescription":"The {tnn} became Squiddish.","mod":"SQUIDDISH","modType":0}},{"id":"team_favor_avoidance","type":92,"title":"Team Avoidance Crate","description":"Your Team will not swing with 9+ Runs.","metadata":{"eventDescription":"The {tnn} gained Avoidance.","mod":"AVOIDANCE","modType":0}},{"id":"team_favor_containment","type":92,"title":"Team Containment Crate","description":"Your Team will gain Containment.","metadata":{"eventDescription":"The {tnn} gained Containment.","mod":"CONTAINMENT","modType":0}}],"blessings":[{"id":"team_boost","type":160,"value":1,"metadata":{"sourceLocation":[0,1,2],"min":".06","max":".06","rating":5},"title":"Season 1:\nBooster Pack","subheader":"Brought to you by@Blaseball Cares for Rainbow Railroad@https://donate.rainbowrailroad.org/fundraiser/3337149","description":"Your Team goes to a Team-building workshop. Boost your entire Team by 6%."},{"id":"random_pitcher_arm_cannon","type":175,"value":1,"metadata":{"sourceLocation":[1],"playerSelection":0,"item":"literal_arm_cannon"},"title":"Season 2:\nLiteral Arm Cannon","description":"A random Player in your Team's Rotation with an available Item slot will gain a Literal Arm Cannon."},{"id":"reroll_worst_pitchers_3","type":11,"value":3,"title":"Season 3:\nExploratory Surgeries","description":"Re-rolls the 3 worst Pitchers in your Team's Rotation."},{"id":"random_hitter_star_boost","type":52,"value":3,"title":"Season 4:\nPrecognition","description":"Vision of things to come. 3 Random Players in your Team's Lineup will be boosted by 20%."},{"id":"random_player_fireproof_jacket","type":175,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"item":"fireproof_jacket"},"title":"Season 5:\nFireproof Jacket","description":"A random Player in your Team's Active Roster with an available Item slot will gain a Fireproof Jacket."},{"id":"steal_leaderboard_player_14","type":78,"value":14,"title":"Season 6:\nLottery Pick","description":"Steal the 14th Most Idolized Player in the League."},{"id":"item_sawed_off_bat","type":175,"value":1,"metadata":{"sourceLocation":[0],"playerSelection":0,"item":"iffey_jr"},"title":"Season 7:\nThe Iffey Jr.","description":"A random Player in your Team's Lineup with an available Item slot will gain a Protector Bat of Minimization."},{"id":"steal_leaderboard_player_21","type":78,"value":21,"title":"Season 8:\nBlind Date","description":"Steal the 21st Most Idolized Player in the League."},{"id":"worst_player_big_payouts","type":112,"value":1,"title":"Season 9:\nCredit to the Team","description":"Zero to hero. The worst player on your Team's Active Roster will earn 5x payouts for Idols Snacks."},{"id":"mod_division_walk_park","type":207,"metadata":{"teamSelection":6,"playerSelection":19,"effect":0,"mod":"WALK_IN_THE_PARK","modType":0},"value":1,"title":"Season 10:\nDivisional Walk in the Park","description":"Take a stroll. Your Division Walks on Ball 3 next Season."},{"id":"tarot_xiii","type":136,"value":12,"title":"Season 11:\nXIII","description":""},{"id":"player_add_uncle_plasma","type":137,"value":1,"title":"Season 12:\nUncle Indemnity","description":"Recruit the Hardboiled Uncle Plasma to your Team."},{"id":"division_improve_batting","type":147,"value":0.04,"title":"Season 13:\nHitting Flotation Bubble","description":"Your entire Division's hitting is boosted by 4%."},{"id":"players_targeted_evolution","type":159,"value":2,"title":"Season 14:\nTargeted Evolution","description":"A random Player in your Team's Lineup and a random Player in your Team's Rotation will Advance."},{"id":"mod_team_afterparty_week","type":168,"value":1,"title":"Season 15:\nAfterparty","description":"Whenever your Team is losing during the First Week of Next Season, they will have a chance of gaining Party Time Boosts."},{"id":"random_hitter_item_skateboard","type":175,"value":1,"metadata":{"sourceLocation":[0],"playerSelection":0,"item":"skateboard"},"title":"Season 16:\nSkate Board","description":"A random Player in your Team's Lineup gets a Skate Board."},{"id":"move_best_hitter_to_third","type":164,"value":1,"metadata":{"sourceLocation":[0,1],"destinationLocation":[0],"playerSelection":1,"destinationSelection":0,"index":2,"fromFirst":true},"title":"Season 17:\nSet the Table","description":"Move the Best Overall Hitter in your Team's Lineup or Rotation to the #3 spot in your Team's Lineup."},{"id":"cape_of_containment","type":175,"value":1,"source":3,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"item":"cape_of_containment"},"title":"Season 18:\nCape of Containment","description":"A random Player on your Team's Active Roster will receive a Cape of Containment."},{"id":"random_player_underhanded","type":167,"value":1,"source":3,"metadata":{"sourceLocation":[1],"playerSelection":0,"mod":"UNDERHANDED","modType":0},"title":"Season 19:\nUnderhanded","description":"Make contact, I dare you. Make a random Player in your Team's Rotation Underhanded. Home Runs will be Unruns against them."},{"id":"team_mod_heavyhanded","type":184,"value":1,"metadata":{"eventDescription":"The {tnn} are now Heavy-Handed.","mod":"HEAVY_HANDED","modType":0},"title":"Season 20:\nHeavy-Handed","description":"Your Team becomes Heavy-Handed, making all Items held by Players on the Team positively eDense."},{"id":"magnify_and_ego_boost","type":199,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0},"title":"Season 21:\nBig Head Mode","description":"Magnify and Ego Boost an Ego-less Player on your Team's Active Roster."},{"id":"recruit_one_shadows_player_from_each","type":201,"metadata":{"teamSelection":7,"sourceLocation":[2],"destinationLocation":[2],"effect":3},"title":"Season 22:\nGachapon","description":"Recruit one random Player from each other Team's Shadows to your Team's Shadows."},{"id":"vault_swap","type":206,"value":1,"title":"Season 23:\nVault Swap","description":"Swap a random Player on your Team's Active Roster with a random Vault Legend."},{"id":"magnify_team","type":205,"value":1,"title":"Season 24:\nEnhance","description":"Magnify your Team."}],"wills":[],"decreesToPass":3,"willsToPass":0,"gifts":[{"id":"edense_item_infusion","type":196,"value":1,"metadata":{"element0":"eDense","sourceLocation":[0,1,2],"percent":0.2},"title":"eDense Infusion","description":"Ratings Boost! Add the eDense Element to 20% of the non-eDense Items held by Players on your Team."},{"id":"adense_item_infusion","type":196,"value":1,"metadata":{"element0":"aDense","sourceLocation":[0,1,2],"percent":0.2},"title":"aDense Infusion","description":"Add the aDense Element to 20% of the Items without it held by Players on your Team."},{"id":"soul_patches_10","type":185,"value":1,"metadata":{"sourceLocation":[0,1],"amountMin":1,"amountMax":1,"repeatMin":10,"repeatMax":10},"title":"Soul Patches","description":"Back by Popular Demand! Give 1 Soul to your least Soul Full player. Repeat 10 times."},{"id":"late_to_party_team","type":184,"value":1,"metadata":{"eventDescription":"The {tnn} are equipped with Late to the Party for the rest of the season!","mod":"LATE_TO_PARTY","modType":1},"title":"Late to the Party, Team Edition","description":"Equip your Team with Late to the Party for the rest of the season, to push your Team into the Overbracket!"},{"id":"early_to_party_team","type":184,"value":1,"metadata":{"eventDescription":"The {tnn} are equipped with Early to the Party for the rest of the season!","mod":"EARLY_TO_PARTY","modType":1},"title":"Early to the Party, Team Edition","description":"Equip your Team with Early to the Party for the rest of the season, to push your Team into the Underbracket!"},{"id":"unambitious_solo","type":167,"value":1,"metadata":{"sourceLocation":[0],"playerSelection":0,"mod":"UNAMBITIOUS","modType":1},"title":"Unambitious, Solo Edition","description":"Equip a random Player in your Team's Lineup with Unambitious for this Season, to make the most of a Underbracket run!"},{"id":"ambitious_solo","type":167,"value":1,"metadata":{"sourceLocation":[0],"playerSelection":0,"mod":"AMBITIOUS","modType":1},"title":"Ambitious, Solo Edition","description":"Equip a random Player in your Team's Lineup with Ambitious for this Season, to make the most of a Overbracket run!"},{"id":"bargain_bin","type":188,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"itemSelection":1},"title":"Bargain Bin","description":"Give a random Player on your Team the Best Item from the Bargain Bin!"},{"id":"random_player_steel_chair","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"crate":"steel_chairs"},"title":"Steel Chair Drop","description":"New! A Random Player on your Team's Active Roster with an available Item slot will open a Steel Chair Crate."},{"id":"random_player_squiddish_egg","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"crate":"squiddish_eggs"},"title":"Squiddish Egg Drop","description":"New! A Random Player on your Team's Active Roster with an available Item slot will open a Squiddish Egg Crate."},{"id":"random_player_trader_item","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"crate":"trader_items"},"title":"Trader Item Drop","description":"New! A Random Player on your Team's Active Roster with an available Item slot will open a Trader Item Crate."},{"id":"random_player_fliickerrriiing_item","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":10,"crate":"fliickerrriiing_items"},"title":"Fliickerrriiing Potion Drop","description":"New! The Worst Player (lowest Combined Stars) on your Team's Active Roster with an available Item slot will open a Fliickerrriiing Potion Crate."},{"id":"random_player_phoenix_quill","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"crate":"phoenix_quills"},"title":"Phoenix Quill Drop","description":"New! A Random Player on your Team's Active Roster with an available Item slot will open a Phoenix Quill Crate."},{"id":"random_player_night_vision_sunglasses","type":177,"value":1,"metadata":{"sourceLocation":[0,1],"playerSelection":0,"crate":"night_vision_sunglasses"},"title":"Night Vision Sunglasses Drop","description":"New! A Random Player on your Team's Active Roster with an available Item slot will open a Night Vision Sunglasses Crate."}]}
  }
}
```

## databasePlayerNamesIds
Gets all player names and IDs currently in the league. Must either be a string, or an object.

Defaults to returning an inefficient map of data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databasePlayerNamesIds.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databasePlayerNamesIds.type = "live"
Returns data from Blaseball, time agnostic.

### databasePlayerNamesIds.type = "static"
Returns a static set of data, parsed from `databasePlayerNamesIds.data`.

Example:
```json
{
  "databasePlayerNamesIds": {
    "type": "static",
    "data": [{"id":"1f930140-9fc6-4240-9ef6-e7ac065eaefb","name":"------ -------"},{"id":"71ac04ae-8210-44e2-8cad-cda68b489d13","name":"Abbie Kyser"},{"id":"98a60f2f-cf34-4fcb-89aa-2d61fb5fba60","name":"Abbott Wright"},{"id":"81fcb973-5fae-40a5-b226-0f48291ffc78","name":"Abner Angry"},{"id":"836e9395-3f83-4d42-ba11-a12c08ceb78b","name":"Abner Pothos"},{"id":"0f62c20c-72d0-4c12-a9d7-312ea3d3bcd1","name":"Abner Wood"},{"id":"0792d1ab-cf94-4160-a7ca-9ccdc84c48a2","name":"Acosta Coleman"},{"id":"d0d7b8fe-bad8-481f-978e-cb659304ed49","name":"Adalberto Tosser"},{"id":"76153b7f-e712-4bb6-bc00-27c830d875fc","name":"Adam Steggi"},{"id":"b643a520-af38-42e3-8f7b-f660e52facc9","name":"Adelaide Judochop"},{"id":"09b7742e-814d-489b-ac2a-ee28eaa906b7","name":"Adeline Catpashman"},{"id":"3d52ace6-a0a0-467e-80e0-ae86b289ad32","name":"Adeline Hammer"},{"id":"11938987-8508-4a4e-ae24-bcc9f691eb5a","name":"Adeline Norindr"},{"id":"e49b9a6c-9493-430b-83c8-11b73e26b5a4","name":"Adi Brothers"},{"id":"5149c919-48fe-45c6-b7ee-bb8e5828a095","name":"Adkins Davis"},{"id":"7951836f-581a-49d5-ae2f-049c6bcc575e","name":"Adkins Gwiffin"},{"id":"113f47b2-3111-4abb-b25e-18f7889e2d44","name":"Adkins Swagger"},{"id":"82733eb4-103d-4be1-843e-6eb6df35ecd7","name":"Adkins Tosser"},{"id":"20395b48-279d-44ff-b5bf-7cf2624a2d30","name":"Adrian Melon"},{"id":"109454c9-5adc-4908-908a-b395be0e0cbf","name":"Adrianna Michet"},{"id":"7331e060-ed21-42b2-a024-3a27abae5919","name":"Adrianna Pinceau"},{"id":"0f6e8b92-304d-4720-b650-9af5150fa9bd","name":"Adrianna Spheroid"},{"id":"5a694dd9-2c47-464d-ae4a-6e0fe8614458","name":"Aeguir Sundae"},{"id":"4f615ee3-4615-4033-972f-79200f9db6e3","name":"Agan Espinoza"},{"id":"f4a5d734-0ade-4410-abb6-c0cd5a7a1c26","name":"Agan Harrison"},{"id":"5cb75330-9e2c-4393-a5de-71ecd19ab9a1","name":"Agnes Tenderson"},{"id":"f479b20b-490a-4884-b379-b9a47d117b14","name":"Agustín Hatchler"},{"id":"28ca3343-f315-4a39-a74f-e2423b4faf08","name":"Aisha Saathoff"},{"id":"e520cfe3-ec6e-44c6-8fb8-9afc2e966e1c","name":"Aisha Thompson"},{"id":"48a80e22-4871-4d1b-a68a-cbd40dc75091","name":"Aitor Ampersand"},{"id":"564dcf65-acad-4615-b484-bee6870b5552","name":"Aitor Kingbird"},{"id":"f2468055-e880-40bf-8ac6-a0763d846eb2","name":"Alaynabella Hollywood"},{"id":"8d81b190-d3b8-4cd9-bcec-0e59fdd7f2bc","name":"Albert Stink"},{"id":"756683e4-ec2e-4169-bff8-ef8ce341cf2f","name":"Alby Saha"},{"id":"e846cba2-31b3-4e30-aaee-de490c35ac84","name":"Aldo Bartlette"},{"id":"cc725a58-38cc-42af-9ff8-ace76541ac26","name":"Aldo Comeback"},{"id":"c0b39229-1f79-4d8a-8444-d418017aa43a","name":"Aldo LaBelle"},{"id":"4bda6584-6c21-4185-8895-47d07e8ad0c0","name":"Aldon Anthony"},{"id":"efafe75e-2f00-4418-914c-9b6675d39264","name":"Aldon Cashmoney"},{"id":"194a78fd-3aa7-4356-8ba0-b9fdcbc0ea85","name":"Aldon Cashmoney II"},{"id":"8ed5b2ff-7725-4c6c-9845-0b821d8aa46f","name":"Aldon Cashmoney III"},{"id":"e5fda841-73bf-4fd1-b460-2702d6d55115","name":"Aldon Cashmoney IV"},{"id":"f741dc01-2bae-4459-bfc0-f97536193eea","name":"Alejandro Leaf"},{"id":"5991ccb3-7eed-46d9-9d7c-69dec9b56d4b","name":"---a Lem-a"},{"id":"c9e4a49e-e35a-4034-a4c7-293896b40c58","name":"Alexander Horne"},{"id":"262c49c6-8301-487d-8356-747023fa46a9","name":"Alexandria Dracaena"},{"id":"34e1b683-ecd5-477f-b9e3-dd4bca76db45","name":"Alexandria Hess"},{"id":"e111a46d-5ada-4311-ac4f-175cca3357da","name":"Alexandria Rosales"},{"id":"2765a648-4993-44a4-b6ae-258ffe18c8c1","name":"Alexi McG"},{"id":"8c5db31e-21b9-4a11-acb7-3e89f3756e1a","name":"Alford Pliskin"},{"id":"94e1e162-9d2e-4407-803a-19bcee369933","name":"Algebra Goldberg"},{"id":"7118ca3f-413d-4336-90d2-af8fbfc1e8f2","name":"Alison Horne"},{"id":"c3945eb7-21df-42ec-b925-5861c13ca2fe","name":"Allie Pancakes"},{"id":"1068f44b-34a0-42d8-a92e-2be748681a6f","name":"Allison Abbott"},{"id":"b477519d-432e-4d14-bf66-6d309b8ca8d8","name":"Allis Wells"},{"id":"b628e10e-ad1f-4d6f-879d-2bbfa5213367","name":"Almond Nugget"},{"id":"611d18e0-b972-4cdd-afc2-793c56bfe5a9","name":"Alston Cerveza"},{"id":"5f3b5dc2-351a-4dee-a9d6-fa5f44f2a365","name":"Alston England"},{"id":"97ec5a2f-ac1a-4cde-86b7-897c030a1fa8","name":"Alston Woods"},{"id":"92497eb4-4cb7-43f7-a32e-60b969784144","name":"Alvie Kesh"},{"id":"b8d39dbe-2681-4fba-b898-bd1824a14c0c","name":"Alvie Tugboat"},{"id":"070758a0-092a-4a2c-8a16-253c835887cb","name":"Alx Keming"},{"id":"80de2b05-e0d4-4d33-9297-9951b2b5c950","name":"Alyssa Harrell"},{"id":"3be2c730-b351-43f7-a832-a5294fe8468f","name":"Amaya Jackson"},{"id":"d934fec9-1a2a-47f9-a33c-fc5c27f69a84","name":"Amias Crunch"},{"id":"8f11ad58-e0b9-465c-9442-f46991274557","name":"Amos Melon"},{"id":"a1137ba7-f057-4bd6-9627-c4624b194901","name":"Anabela Henriques"},{"id":"21555bfb-6aed-4510-863a-801be3b6d997","name":"Anastasia Isarobot"},{"id":"c0c85be4-ff26-470f-8135-af771fd21e51","name":"Anathema Elemefayo"},{"id":"ac4a94bf-ef7d-4b88-b66e-0e0864e673b3","name":"Anathema Park"},{"id":"5c2d12bc-088f-4930-85e8-a0aef16ecff6","name":"Anaximandra Metzger"},{"id":"f38c5d80-093f-46eb-99d6-942aa45cd921","name":"Andrew Solis"},{"id":"57b9ae39-1471-460b-94c9-75e078d9989e","name":"Anemone Pancakes"},{"id":"d6278afa-9d11-4f46-b57e-86169bdcd0e0","name":"Aneurin Ashwell"},{"id":"fe1f826f-7346-425c-9d3c-7ed5a7eae1b7","name":"Angel Ivories"},{"id":"56eba639-ea73-474d-bcee-ec68c0528b02","name":"Ankle Halifax"},{"id":"1719f12b-2750-48cc-955a-0eb453bec361","name":"Anna Trefeather"},{"id":"6ccc65e7-817d-4528-9757-b81d327d71e2","name":"Annick Throgmorten"},{"id":"4f7d7490-7281-4f8f-b62e-37e99a7c46a0","name":"Annie Roland"},{"id":"23e78d92-ee2d-498a-a99c-f40bc4c5fe99","name":"Annie Williams"},{"id":"f2ec4c2b-ec91-4bc1-9a37-fd612e7e7d1d","name":"Anthony Gibas"},{"id":"efa73de4-af17-4f88-99d6-d0d69ed1d200","name":"Antonio Mccall"},{"id":"248ccf3d-d5f6-4b69-83d9-40230ca909cd","name":"Antonio Wallace"},{"id":"432ea93d-bcb9-4f01-8d93-5d0d44fea98a","name":"Aoife Mahle"},{"id":"825be53e-c5eb-4125-bee4-0c898549edf5","name":"Arantxa Inagame"},{"id":"43e4de02-cc21-4413-bb88-1c1128eec174","name":"Arbutus Bones"},{"id":"84a17f48-0393-4166-864a-f7cd27557701","name":"Arches Candle"},{"id":"16334652-e342-4805-bc8e-f149dbe6b96a","name":"Arches Manhattan"},{"id":"ec029d42-3fcb-425a-b719-aad45423e1d4","name":"Arches Schumacher"},{"id":"fc9637a2-978d-4ea9-b632-888d6a86148a","name":"Archie Lampman"},{"id":"e9d9cd83-8e32-4372-bda5-b06181ce9424","name":"Ardy Salt"},{"id":"293c2f48-94d3-4723-808b-52a00ddadc9f","name":"Ardy Wells"},{"id":"b3682007-4275-4e1a-ac98-24ae728d43a1","name":"Ariadne Bedframe"},{"id":"b14bae53-80ef-43cf-a117-3c9e7d223464","name":"Armen Frosting"},{"id":"9337389f-4301-4e30-a108-4680c02ee1fe","name":"Artemesia Teixeira"},{"id":"8c9c7827-9b20-49f2-8e64-cb1ff09351b0","name":"Artemesia Watts"},{"id":"f78ba0e2-0d21-474e-ab34-b2d475c85ad1","name":"Arthur Bath"},{"id":"e672891d-eaf9-45cc-aee9-3349d6ea9e12","name":"Arthur Clark"},{"id":"a35abefc-16db-499a-b82b-e2ef3c34ee4c","name":"Arthur Solo"},{"id":"6f9de777-e812-4c84-915c-ef283c9f0cde","name":"Arturo Huerta"},{"id":"ddcd33e0-b888-4cfb-b29d-70fd751b4b7b","name":"Arturo Muggins"},{"id":"ea232e74-96a9-4549-a511-1077a1a7d95b","name":"Arvin Star"},{"id":"bdde3fcb-1142-4c71-a44b-d912cecc6b00","name":"Astrothesia Shufflecat"},{"id":"1805fe71-1738-4bc8-9362-0a79557e7ceb","name":"Athena Nopales"},{"id":"c6146c45-3d9b-4749-9f03-d4faae61e2c3","name":"Atlas Diaz"},{"id":"d46abb00-c546-4952-9218-4f16084e3238","name":"Atlas Guerra"},{"id":"f44a8b27-85c1-44de-b129-1b0f60bcb99c","name":"Atlas Jonbois"},{"id":"2b9d6b50-3ce2-4cb1-8cd1-2a0fc05733e3","name":"Atma Blueberry"},{"id":"47f0ccd8-3ee3-47c7-b00d-42416cfd1344","name":"Atma Truk"},{"id":"b93f4c32-64e4-4461-b708-05f6f7f6bbc2","name":"Atma Willowtree"},{"id":"2ab5811d-a5dd-4059-ac4c-5cbc39fc537c","name":"Attila Wyeth"},{"id":"b0421cb2-e653-4581-9b83-04c8381e58c0","name":"Aubrey Teakwook"},{"id":"0a1d221a-f4b2-4691-bb08-c7757659f6c9","name":"Aubrey Wildarms"},{"id":"a9812a8e-67c4-434c-85cb-6ddf785cadf2","name":"Augusta Chadwell"},{"id":"d3cdafd0-4026-4af8-b744-6982a0bfe3fa","name":"Augusta Sunset"},{"id":"c17a4397-4dcc-440e-8c53-d897e971cae9","name":"August Mina"},{"id":"d8bc482e-9309-4230-abcb-2c5a6412446d","name":"August Obrien"},{"id":"ae81e172-801a-4236-929a-b990fc7190ce","name":"August Sky"},{"id":"a083246b-35f4-4dad-8b07-8b28047e0823","name":"Aura Irving"},{"id":"9de086f9-7b67-46c5-a017-89c98788d0d4","name":"Aureliano Castillo"},{"id":"57b4827b-26b0-4384-a431-9f63f715bc5b","name":"Aureliano Cerna"},{"id":"4aa843a4-baa1-4f35-8748-63aa82bd0e03","name":"Aureliano Dollie"},{"id":"e749dc27-ca3b-456e-889c-d2ec02ac7f5f","name":"Aureliano Estes"},{"id":"99987899-7e13-44b2-a13c-462d224840ab","name":"Aureliano Lenny"},{"id":"1f0f2e1e-79b9-4e1b-afe4-1ff8717a0149","name":"Aureliano Sparks"},{"id":"2e94fa0d-453f-4925-8ea0-c0a53e38108d","name":"Aurora Blortles"},{"id":"2918be01-e1aa-4de6-8239-fe62f37769de","name":"Avi Jones"},{"id":"8b0d717f-ae42-4492-b2ed-106912e2b530","name":"Avila Baker"},{"id":"8cf78b49-d0ca-4703-88e8-4bcad26c44b1","name":"Avila Guzman"},{"id":"4e63cb5d-4fce-441b-b9e4-dc6a467cf2fd","name":"Axel Campbell"},{"id":"4204c2d1-ca48-4af7-b827-e99907f12d61","name":"Axel Cardenas"},{"id":"3af96a6b-866c-4b03-bc14-090acf6ecee5","name":"Axel Trololol"},{"id":"19241aa4-38e3-45ed-9750-68f4401f80e1","name":"Ayanna Dumpington"},{"id":"05bf598f-94e2-4729-8154-0130c7f212c7","name":"Aymer Bergeron"},{"id":"128a33b9-c82f-408e-9fa3-5161f5a3e297","name":"Aymer Eggleton"},{"id":"678170e4-0688-436d-a02d-c0467f9af8c0","name":"Baby Doyle"},{"id":"14bfad43-2638-41ec-8964-8351f22e9c4f","name":"Baby Sliders"},{"id":"ad8d15f4-e041-4a12-a10e-901e6285fdc5","name":"Baby Triumphant"},{"id":"95007c81-012f-4068-84f3-e577348e61b5","name":"Backpatch Lincecum"},{"id":"4dc3ea1a-f008-44de-9fbe-4811552ef662","name":"Backpatch Rolsenthal"},{"id":"520b2a14-ef60-4ee8-9f39-c7be12971ef2","name":"Backwoods Broker"},{"id":"f22482e8-784a-4b6e-b5ad-06f37554b89b","name":"Badger Powers"},{"id":"1b2653a7-602a-4688-83ab-f68e23c79528","name":"Badgerson Bedazzle"},{"id":"9ef09db2-da89-44a7-b41b-1348775919b8","name":"Badgerson Stromboli"},{"id":"e4034192-4dc6-4901-bb30-07fe3cf77b5e","name":"Baldwin Breadwinner"},{"id":"ebf2da50-7711-46ba-9e49-341ce3487e00","name":"Baldwin Jones"},{"id":"85e30695-d80c-42f0-a3df-32731942fb24","name":"Balina Erock"},{"id":"1a53768b-1ec1-4646-8417-dd58b9849bd7","name":"Ball Clark"},{"id":"2fe49eb9-8709-421e-9040-4ab6afb0191d","name":"Balthazar Crikey"},{"id":"62111c49-1521-4ca7-8678-cd45dacf0858","name":"Bambi Perez"},{"id":"003b6d29-1797-4762-a4ba-5ff574aeaa43","name":"Bandit Marama"},{"id":"b4a0b0f6-9e2d-4a18-bcdc-969dc211703a","name":"Barney Mocha"},{"id":"6fa08e08-6a95-4fbc-9d00-5c39c5302b60","name":"Barry Burkhard"},{"id":"37061380-ac95-4018-854e-c308073945e9","name":"Bartleby Zhivago"},{"id":"bad4e663-b075-4003-83b8-e0ec799f37fd","name":"Bash Wellerman"},{"id":"d0ffac42-a19b-404e-b54f-8bb5a6f14ad3","name":"Basil Foamcore"},{"id":"c755efce-d04d-4e00-b5c1-d801070d3808","name":"Basilio Fig"},{"id":"27c68d7f-5e40-4afa-8b6f-9df47b79e7dd","name":"Basilio Mason"},{"id":"9965eed5-086c-4977-9470-fe410f92d353","name":"Bates Bentley"},{"id":"945974c5-17d9-43e7-92f6-ba49064bbc59","name":"Bates Silk"},{"id":"e181ad4a-dd09-4873-ae68-b50518d20722","name":"Batista Oatmilk"},{"id":"b9293beb-d199-4b46-add9-c02f9362d802","name":"Bauer Zimmerman"},{"id":"af161a5a-c73e-4aec-94d1-ac6b8e13b898","name":"Beaker Barley "},{"id":"1ac63473-6150-4ade-9efd-75ad424265e5","name":"Beaker Windchime"},{"id":"dddb6485-0527-4523-9bec-324a5b66bf37","name":"Beans McBlase"},{"id":"bbdeb3e2-cd5a-46ff-9a69-fd06b1482710","name":"Beans Reblase"},{"id":"26cfccf2-850e-43eb-b085-ff73ad0749b8","name":"Beasley Day"},{"id":"36786f44-9066-4028-98d9-4fa84465ab9e","name":"Beasley Gloom"},{"id":"ab9b2592-a64a-4913-bf6c-3ae5bd5d26a5","name":"Beau Huerta"},{"id":"e972984c-2895-451c-b518-f06a0d8bd375","name":"Becker Solis"},{"id":"7a75d626-d4fd-474f-a862-473138d8c376","name":"Beck Whitney"},{"id":"2f85d731-81ed-4a07-9e01-e93f1786a366","name":"Bees -aswell"},{"id":"8b5ddd3d-cca6-4e01-ac7f-65dc56831933","name":"Bees Gorczyca"},{"id":"293deb94-e4c4-4ea8-b954-8233ddc05a87","name":"Belinda Barios"},{"id":"aef42525-591e-4cc7-b1e9-f69a10d9e5a8","name":"Benjamin Delacruz"},{"id":"fcb08e4f-df3a-446c-ab50-58a496ac5f3f","name":"Bennett Bluesky"},{"id":"13cfbadf-b048-4c4f-903d-f9b52616b15c","name":"Bennett Bowen"},{"id":"03097200-0d48-4236-a3d2-8bdb153aa8f7","name":"Bennett Browning"},{"id":"16a38c85-92fd-4db9-a186-395e84af2b26","name":"Bennett Cornbread"},{"id":"fb997c06-23f7-4893-87dd-712a5afba567","name":"Bennett Laplace"},{"id":"0d8076a7-4910-4fec-bd48-f22aaa567457","name":"Bennett McClutch"},{"id":"b056a825-b629-4856-856b-53a15ad34acb","name":"Bennett Takahashi"},{"id":"799a254c-ae53-4959-8533-2fc3bede66fb","name":"Benny Yardstick"},{"id":"cd218f25-b4c9-4357-b3fb-a93ba81f457e","name":"Benson Evergreen"},{"id":"e02fd171-5a6a-45d8-9c5f-ecd80e649bc1","name":"Bernie Ji"},{"id":"15b1b9d3-4921-4898-9f71-e33e8e11cae7","name":"Bert Wyeth"},{"id":"ceac785e-55fd-4a4e-9bc8-17a662a58a38","name":"Best Cerna"},{"id":"1c49ea43-7c93-41ff-9bcb-59a2928527bd","name":"Bethel Swan"},{"id":"1732e623-ffc2-40f0-87ba-fdcf97131f1f","name":"Betsy Trombone"},{"id":"e6114fd4-a11d-4f6c-b823-65691bb2d288","name":"Bevan Underbuck"},{"id":"ac69dba3-6225-4afd-ab4b-23fc78f730fb","name":"Bevan Wise"},{"id":"f72647b4-49a0-4bd5-9f17-43c45cd9c1ce","name":"Bevis Blounder"},{"id":"a1b55c5f-6d01-4ca1-976a-5cdfe18d199c","name":"Billup Kiddo"},{"id":"42ac4519-eb2a-41bf-b85a-c75129776257","name":"Bistro Mustard"},{"id":"331d9910-0991-40c8-9345-a7cddb7465c6","name":"Bistro Succotash"},{"id":"f462061d-d8cb-4505-9a12-b083f790c442","name":"Blaire Frihart"},{"id":"9b751c3c-6c40-4a20-bd00-3a1011802ee0","name":"Blaire Nash"},{"id":"63d5845d-f077-44ca-91fd-e33ac85af560","name":"Blake Moss"},{"id":"7b0f91aa-4d66-4362-993d-6ff60f7ce0ef","name":"Blankenship Fischer"},{"id":"afdbd837-7fc8-4c97-ac31-636950c2b3e4","name":"Blaseball Surgeon"},{"id":"d259402f-98f3-44dc-b848-e432d0ae63a3","name":"Blimp Hardison"},{"id":"88da6cac-0cac-433b-b08c-a0c7a4fb488f","name":"Blondie Kimball"},{"id":"f7847de2-df43-4236-8dbe-ae403f5f3ab3","name":"Blood Hamburger"},{"id":"88deffb6-9f94-42a5-9f8c-f528b37037b4","name":"Bloom Goblin"},{"id":"3ed9c552-a9a4-4cb3-8d7d-e1a2114f8515","name":"Blossom Fiasco"},{"id":"5d3c5190-967f-4711-9542-9f03b6978f4a","name":"Blossom LaBelle"},{"id":"99ab095d-7871-4831-b9d6-cff6bb0a930e","name":"Blossom Mondegreen"},{"id":"5ddee49d-79f2-4eba-a3c9-174784322059","name":"Bobbin Inningson"},{"id":"87d5d848-a1b1-4e3e-b3ab-5c47433f066e","name":"Bobbin Moss"},{"id":"0892da6e-c9d1-494f-84bc-0c1f879018c0","name":"Bob E. Cagayan"},{"id":"4cd63a32-7fae-4e79-bd6b-5d8740ec3529","name":"Bob Marzen"},{"id":"1744f78f-783d-421a-831f-246b99586abc","name":"Boden Anene"},{"id":"44e0f741-f121-4124-ab77-f06a8827a250","name":"Bogan Beanpot"},{"id":"11a2a8a7-8c92-447b-9448-2ec5ac93bf65","name":"Bones Piazza"},{"id":"8f450b00-334a-40a2-a445-f387937956a3","name":"Bonito Sanchez"},{"id":"f66fac56-fc42-4544-803a-0f84003d2eab","name":"Bonito Statter Jr."},{"id":"2ffbaec8-a646-4435-a17a-3a0a2b5f3e16","name":"Bonk Jokes"},{"id":"7e06b07d-8d11-4871-999c-5c0e0d90bfac","name":"Bonnie Blueglass"},{"id":"c3ae0552-59e8-44bf-ba66-48a96aff35e6","name":"Bontgomery Mullock"},{"id":"f2bf707d-5c40-4364-90ba-8d9e8348fca2","name":"Borg Ruiz"},{"id":"6b2cf5b5-88bf-437e-b81d-51e70d942230","name":"Bortimus Celestina"},{"id":"9c813008-b2a9-4257-80e7-a709c67189fe","name":"Bottles Suljak"},{"id":"9032c905-5dec-4bb8-9140-47cfd7f16940","name":"Boudicca Midcentury"},{"id":"8cfb7ee9-cfa4-445c-830e-2723797fa7b3","name":"Boxplot Jones"},{"id":"e25ba8a8-c084-4141-9022-508cc30f7f83","name":"Boyd English"},{"id":"16a59f5f-ef0f-4ada-8682-891ad571a0b6","name":"Boyfriend Berger"},{"id":"493a83de-6bcf-41a1-97dd-cc5e150548a3","name":"Boyfriend Monreal"},{"id":"753a6d17-3fc0-4572-879b-d0d51a1f86fe","name":"BQ Droodle"},{"id":"65d8dc6e-a85b-49c5-b9db-9d8767aee7cc","name":"Brad Mauser"},{"id":"699c9b2f-c3d8-4211-b25e-63170c2ed9a8","name":"Brad Woomy"},{"id":"4ba50817-d477-438e-af14-824ee6bfa175","name":"Branson Youngblood"},{"id":"82dcd0de-3a41-46b7-979b-98e9b75b3f7a","name":"Breckon Gallant"},{"id":"cef92ffa-de37-41b8-889e-ad65d1a978b8","name":"Bree Cuthbert"},{"id":"3c53ff1c-6437-4965-a4a2-489bcb91d3f4","name":"Breeze Regicide"},{"id":"3de17e21-17db-4a6b-b7ab-0b2f3c154f42","name":"Brewer Vapor"},{"id":"9e7cf663-23a2-4fa7-a2e3-8cac6af5d9c4","name":"Bria Snickers"},{"id":"60026a9d-fc9a-4f5a-94fd-2225398fa3da","name":"Bright Zimmerman"},{"id":"b93bdf99-d9b2-4717-9798-63bd22b0d7da","name":"Brimtley Tokkan"},{"id":"5f5764c7-c3a0-4dae-ad17-c6689f1e8c27","name":"Brisket Friendo"},{"id":"97dfc1f6-ac94-4cdc-b0d5-1cb9f8984aa5","name":"Brock Forbes"},{"id":"e22a3609-e94d-4e7c-a9eb-2064e3261116","name":"Brock Harvie"},{"id":"4542f0b0-3409-4a4a-a9e1-e8e8e5d73fcf","name":"Brock Watson"},{"id":"55886358-0dc0-4666-a483-eecb4e95bd7e","name":"Bront Dickerson"},{"id":"71b1f994-3767-4c58-aace-b5ba1210d911","name":"Brooke Lingardo"},{"id":"0fe896e1-108c-4ce9-97be-3470dde73c21","name":"Bryanayah Chang"},{"id":"81efd97c-ba37-4b00-b9af-1a3956a327bb","name":"Bryce Barlow"},{"id":"af140250-42d3-44c8-9ba5-f3852dffa73e","name":"Brynn Chamberlain"},{"id":"382d9269-5cd8-4125-a23a-459a640bbaf3","name":"Brynn Halifax"},{"id":"61e58265-b673-44f3-b263-f642a658b1ab","name":"B. Tugboat"},{"id":"e56fe417-7385-4eee-8db8-b9eb71410621","name":"Buck Denardi"},{"id":"38f3ba48-47aa-4116-be5f-91fbcebd82f7","name":"Buck Humdinger"},{"id":"f617c1ef-d676-4119-8359-a5c3ac3bef51","name":"Buck Latenight"},{"id":"30b7053a-7f5f-4e3b-9600-7b3da8ba7dc0","name":"Buck Rattler"},{"id":"0a1c3949-8927-4eae-a189-f4e073955ba4","name":"Buddy Herrold"},{"id":"9e39f808-ff70-4232-8a8c-28085227155f","name":"Bugcatcher Roldan"},{"id":"65273615-22d5-4df1-9a73-707b23e828d5","name":"Burke Gonzales"},{"id":"679e0b2f-fabf-4e95-a92d-73df2d9c5c7f","name":"Butch Charcuterie"},{"id":"13cf5521-140f-4ad9-a998-ac8af22b9bc8","name":"Butch Wyeth"},{"id":"21ea41b6-c122-46d9-ac09-3e23caad17f0","name":"Byung-Hyun Octothorp"},{"id":"09847345-5529-4766-9f4a-ed6fefc97b01","name":"Cadence Murphy"},{"id":"1513aab6-142c-48c6-b43e-fbda65fd64e8","name":"Caleb Alvarado"},{"id":"cd6b102e-1881-4079-9a37-455038bbf10e","name":"Caleb Morin"},{"id":"0eddd056-9d72-4804-bd60-53144b785d5c","name":"Caleb Novak"},{"id":"f7715b05-ee69-43e5-a0e5-8e3d34270c82","name":"Caligula Lotus"},{"id":"f3c07eaf-3d6c-4cc3-9e54-cbecc9c08286","name":"Campos Arias"},{"id":"c0998a08-de15-4187-b903-2e096ffa08e5","name":"Cannonball Sports"},{"id":"3822990b-5ee0-404a-9e06-846bb29f3faf","name":"Cantus Hojo"},{"id":"fbdbd9ac-37ac-4cd9-9e57-ae619cef8e49","name":"Cardamom Sokol"},{"id":"3fe1bce5-dbd1-40af-a116-5687f2a3281d","name":"Carl Davis"},{"id":"c18961e9-ef3f-4954-bd6b-9fe01c615186","name":"Carmelo Plums"},{"id":"1e07ea2e-5114-4c35-89d5-803c696a1fa3","name":"Carol Swine"},{"id":"92af85c2-78e2-460f-b1a7-462f95436de5","name":"Carrol Coopwood"},{"id":"50154d56-c58a-461f-976d-b06a4ae467f9","name":"Carter Oconnor"},{"id":"6524e9e0-828a-46c4-935d-0ee2edeb7e9a","name":"Carter Turnip"},{"id":"90cc0211-cd04-4cac-bdac-646c792773fc","name":"Case Lancaster"},{"id":"8d337b47-2a7d-418d-a44e-ef81e401c2ef","name":"Case Sports"},{"id":"0fbffd5c-cf7c-49c0-a579-4daec23bff4d","name":"Cassidy Rochester"},{"id":"0672a4be-7e00-402c-b8d6-0b813f58ba96","name":"Castillo Logan"},{"id":"defbc540-a36d-460b-afd8-07da2375ee63","name":"Castillo Turner"},{"id":"b357fbf4-533e-4f2c-8616-a576e9954795","name":"Cat Inning"},{"id":"87f5c53a-46fe-41a6-b3f7-7d3dc90ac1bf","name":"Cat McSriff"},{"id":"6d9001ff-ba9f-40c0-9315-79feba541b65","name":"Catmint Chen"},{"id":"6fc3689f-bb7d-4382-98a2-cf6ddc76909d","name":"Cedric Gonzalez"},{"id":"c31d874c-1b4d-40f2-a1b3-42542e934047","name":"Cedric Spliff"},{"id":"a13f67d5-22eb-4ee9-8b67-893b21acd87b","name":"Cedrissimo Sugar"},{"id":"821f79d2-1043-4a7f-9b24-086aff365b15","name":"Celeste Laurie"},{"id":"bd8778e5-02e8-4d1f-9c31-7b63942cc570","name":"Cell Barajas"},{"id":"4fe28bc1-f690-4ad6-ad09-1b2e984bf30b","name":"Cell Longarms"},{"id":"c793f210-4ec1-421f-afe7-7ee729799624","name":"Cell Ramsey"},{"id":"94258859-be2b-43c0-8ec3-2a8c00c1bbd1","name":"Celo Braga"},{"id":"aee14879-b924-450a-9eec-bfef97c575ab","name":"Chadwick Flex"},{"id":"e3e1d190-2b94-40c0-8e88-baa3fd198d0f","name":"Chambers Kennedy"},{"id":"a647388d-fc59-4c1b-90d3-8c1826e07775","name":"Chambers Simmons"},{"id":"87614491-a213-411c-ad75-e02aac025366","name":"Chambers Smith"},{"id":"800ac627-0334-41d5-98df-c51a47e46aef","name":"Chandra Blortles"},{"id":"56f4d8e8-ddb2-4ae7-8691-d74fd85faf50","name":"Chandra Wildarms"},{"id":"82d5e79d-e125-4460-b1fa-d046ad7739e0","name":"Changeup Liu"},{"id":"c4dec95e-78a1-4840-b209-b3b597181534","name":"Charlatan Seabright"},{"id":"1d550329-ac65-488e-a8b7-c131832eb601","name":"Chester Datalake"},{"id":"3017db8b-b627-4d33-a51c-e4045a17887c","name":"Chet Chin"},{"id":"cc933b79-9218-4693-8172-f23d4eaccdf7","name":"Chet Takahashi"},{"id":"b2eb76eb-2c34-4184-b87d-4620faec8ad8","name":"Chibodee Alighieri"},{"id":"81b9a3d2-3344-4b1e-8a08-7994aa640467","name":"Chip Alstott"},{"id":"ba5bc79a-afa3-47d9-a263-272cc2901d35","name":"Chips Painter"},{"id":"a3947fbc-50ec-45a4-bca4-49ffebb77dbe","name":"Chorby Short"},{"id":"a1628d97-16ca-4a75-b8df-569bae02bef9","name":"Chorby Soul"},{"id":"e7bbfb62-a138-41e8-86af-b91843d17013","name":"Chorby Soul II"},{"id":"b28bb7f7-2d8c-4781-8808-83844df7e732","name":"Chorby Soul III"},{"id":"c817c6cc-8574-4857-83f1-4a311fa89258","name":"Chorby Soul IV"},{"id":"0ac5f3da-8acf-4b8e-84ad-99fa20fd2105","name":"Chorby Soul V"},{"id":"b9615747-4710-4dbe-a68e-b376c64dc10a","name":"Chorby Soul VI"},{"id":"9bd4e070-a5d9-4f8d-92eb-fb2b469f30a7","name":"Chorby Soul VII"},{"id":"0e27df51-ad0c-4546-acf5-96b3cb4d7501","name":"Chorby Spoon"},{"id":"4562ac1f-026c-472c-b4e9-ee6ff800d701","name":"Chris Koch"},{"id":"c4418663-7aa4-4c9f-ae73-0e81e442e8a2","name":"Chris Thibault"},{"id":"9ac2e7c5-5a34-4738-98d8-9f917bc6d119","name":"Christian Combs"},{"id":"392078c0-0c69-4b71-ba61-3276eb00896b","name":"Christopher Yuki"},{"id":"5b13308b-0658-4c94-ab8d-5b72a0d545ed","name":"Chris Vainglory"},{"id":"6c1c0539-4168-4977-bf17-257bcbfec92b","name":"Chromatic Jump"},{"id":"077d4fd6-6396-46bd-9f85-3d6938f5657e","name":"Cicero Gubbins"},{"id":"75e80e46-276d-4388-bb6a-da9125a0e8ad","name":"Cicero Homestyle"},{"id":"7a476b05-5713-433c-8779-20be74551267","name":"Cicero Huhtala"},{"id":"060ce5c7-93bf-4ee7-a2fb-be86736cac1e","name":"Cindy Glass"},{"id":"b510c829-2408-4908-8c12-810d39deea9a","name":"Cinnamon Peep"},{"id":"7d45e942-ac28-41b6-8d89-5e4a44591c11","name":"Cinna Toast"},{"id":"49a7299b-25e7-450a-989e-d45572a62383","name":"Cissy Heartlight"},{"id":"2efb1f0a-72de-4178-9e53-35cd3e9f7798","name":"Clare Ballard"},{"id":"a772e638-f5ee-4e29-8424-9301ba93d5b0","name":"Clare Ballard II"},{"id":"8592446a-51fe-41d1-876f-c8cf1bde28c4","name":"Clare Ballard III"},{"id":"88ca603e-b2e5-4916-bef5-d6bba03235f5","name":"Clare Mccall"},{"id":"c5d1e6f2-f8f7-48fe-bc3d-3aa42b44788d","name":"Claudio Balton"},{"id":"ec41d37e-2398-4e7d-aaf0-f7f52e5300c7","name":"Clementine Steeplechase"},{"id":"17012262-1ea7-474c-b2fa-0723119e125b","name":"Clodius Applesauce"},{"id":"73aa5478-ec4c-4ec3-8a96-984d57539f0c","name":"Clove Ji-Eun"},{"id":"54bc7b23-49a9-4f1d-b60f-9c3cf9754b67","name":"Clove Mahle"},{"id":"5d5f79ba-bfc8-4bf8-ad0a-d5cd02aebdfc","name":"Clove Rotato"},{"id":"5dbf11c0-994a-4482-bd1e-99379148ee45","name":"C-nrad Va--h-n"},{"id":"787e7290-257c-4ec6-89e2-12b7a13cf5da","name":"Cocoa Weeks"},{"id":"c0210f64-e8a8-4a33-a40c-2b9210a2bfd4","name":"Cody Langzone"},{"id":"ef9f8b95-9e73-49cd-be54-60f84858a285","name":"Collins Melon"},{"id":"51cba429-13e8-487e-9568-847b7b8b9ac5","name":"Collins Mina"},{"id":"8f563500-0b23-4491-a0e9-bb95298e11aa","name":"Colton Nanda"},{"id":"f8c20693-f439-4a29-a421-05ed92749f10","name":"Combs Duende"},{"id":"0295c6c2-b33c-47dd-affa-349da7fa1760","name":"Combs Estes"},{"id":"459f7700-521e-40da-9483-4d111119d659","name":"Comfort Monreal"},{"id":"d8ee256f-e3d0-46cb-8c77-b1f88d8c9df9","name":"Comfort Septemberish"},{"id":"a7d8196a-ca6b-4dab-a9d7-c27f3e86cc21","name":"Commissioner Vapor"},{"id":"89c8f6bf-94a2-4e56-8412-bdb0182509a9","name":"Concrete Mandible"},{"id":"94844fad-9519-4c14-8ab3-d38606a7bb44","name":"Conditional Yuniesky"},{"id":"740d5fef-d59f-4dac-9a75-739ec07f91cf","name":"Conner Haley"},{"id":"6e373fca-b8ab-4848-9dcc-50e92cd732b7","name":"Conrad Bates"},{"id":"ce0a156b-ba7b-4313-8fea-75807b4bc77f","name":"Conrad Twelve"},{"id":"fa40d6ea-e24b-4422-9df1-0643ab202c5f","name":"Coolname Galvanic"},{"id":"22623721-b91d-40bb-9c8b-87e00a6c286a","name":"Corbyn Koeppe"},{"id":"fc9c0ac4-f8ee-4147-a40e-d396ad41864d","name":"Cormorant Catalina"},{"id":"03f920cc-411f-44ef-ae66-98a44e883291","name":"Cornelius Games"},{"id":"fbb5291c-2438-400e-ab32-30ce1259c600","name":"Cory Novak"},{"id":"17397256-c28c-4cad-85f2-a21768c66e67","name":"Cory Ross"},{"id":"475f0058-3cd5-4219-8674-370ec88fa40a","name":"Cory Terermorphasis"},{"id":"2aee32f9-a5bc-4f95-932c-cf7492d09374","name":"Cory Thirteen"},{"id":"2da49de2-34e5-49d0-b752-af2a2ee061be","name":"Cory Twelve"},{"id":"26545c1f-bdb9-41f5-bced-45238b45e9fc","name":"Cote Loveless"},{"id":"fe70fe7e-7f59-451c-8ecb-081549b8397c","name":"Cote Loveless II"},{"id":"aac4947f-4101-4fa4-b71c-090b1cad88d3","name":"Cote Loveless III"},{"id":"b5c95dba-2624-41b0-aacd-ac3e1e1fe828","name":"Cote Rodgers"},{"id":"bde7bdc5-e0a8-4990-a6b6-c4909c3ebbc7","name":"Cousin Spike"},{"id":"ad38b19e-693d-406e-8478-e9cacd739c29","name":"Cravel Gagnon"},{"id":"51dab868-820b-4969-8bba-bde0be8090d7","name":"Cravel Gesundheit"},{"id":"fa58f623-3578-4ffc-8f04-545505e20445","name":"Cricket Buttercup"},{"id":"edd95bea-dd51-416f-9372-22db4c39c140","name":"Cricket Chamomile"},{"id":"43256f39-9db7-4bda-bc10-814a60b4ede2","name":"Crits Manhattan"},{"id":"6f924ae0-1613-488f-add0-bce1eb60845a","name":"Crits Ratoon"},{"id":"da6f2a92-d109-47ad-8e16-854ae3f88906","name":"Crow Grackle"},{"id":"5d1770ff-7366-476e-8e8c-a7b82ed095de","name":"Crow Madrigal"},{"id":"b09945a7-6631-4db8-bec2-72a013fb9441","name":"C-r-u-- S-a-d-ak-"},{"id":"6a567da6-7c96-44d3-85de-e5a08a919250","name":"Cudi Di Batterino"},{"id":"09f2787a-3352-41a6-8810-d80e97b253b5","name":"Curry Aliciakeyes"}]
  }
}
```



## databasePlayers
Gets players with the provided IDs for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databasePlayers.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databasePlayers.type = "live"
Returns data from Blaseball, time agnostic.

### databasePlayers.type = "static"
Returns a static set of data, parsed from `databasePlayers.data`.

Example:
```json
{
  "databasePlayers": {
    "type": "static",
    "data": [{"id":"38a1e7ce-64ed-433d-b29c-becc4720caa1","anticapitalism":0.0866678627331002,"baseThirst":0.10237926985001013,"buoyancy":0.6022793194801744,"chasiness":0.5122617433195847,"coldness":1.0583041307016892,"continuation":0.7692722227151576,"divinity":1.8453709796803361,"groundFriction":0.49456495791182853,"indulgence":0.7991810271247187,"laserlikeness":0.3083274291250653,"martyrdom":0.31609637356221354,"moxie":0.7307417915082235,"musclitude":1.535749789064464,"name":"Parker MacMillan","omniscience":0.27551679016879876,"overpowerment":0.12165592881414125,"patheticism":0.8609999999999999,"ruthlessness":2.012628831803399,"shakespearianism":1.965822935489618,"suppression":0.14100000000000001,"tenaciousness":0.5828231258667219,"thwackability":1.4795201120377561,"tragicness":0.8609999999999999,"unthwackability":0.18611476550937,"watchfulness":0.16517359815256782,"pressurization":0.23393556008324898,"totalFingers":10,"soul":3,"deceased":false,"peanutAllergy":false,"cinnamon":0.057040746496574035,"fate":3,"ritual":"Obsessing over past mistakes","coffee":0,"blood":10,"permAttr":["PROFIT","NONPROFIT","FIREWALKER","NEWADVENTURE","SUPERWANDERER","LEGENDARY"],"seasAttr":[],"weekAttr":[],"gameAttr":[],"hitStreak":0,"consecutiveHits":0,"baserunningRating":0.3924841648450664,"pitchingRating":0.4457954017590105,"hittingRating":1.2445295271736567,"defenseRating":0.4242931625046796,"leagueTeamId":"979aee4a-6d80-4863-bf1c-ee1a78e06024","tournamentTeamId":null,"eDensity":0,"state":{"itemModSources":{"SUPERWANDERER":["eecc9bf3-96b5-4ea9-9a4a-05f0a0d586f0"]}},"evolution":0,"items":[{"id":"eecc9bf3-96b5-4ea9-9a4a-05f0a0d586f0","name":"The Fifth Base","forger":null,"forgerName":null,"prePrefix":null,"prefixes":[{"name":"Super Roamin'","adjustments":[{"mod":"SUPERWANDERER","type":0}]}],"postPrefix":null,"root":{"name":"Base","adjustments":[{"stat":9,"type":1,"value":0.10466822430747254}]},"suffix":null,"durability":-1,"health":1,"baserunningRating":0.02241110148345271,"pitchingRating":0,"hittingRating":0,"defenseRating":0,"state":{}}],"itemAttr":["SUPERWANDERER"]}]
  }
}
```

## databasePlayersByItemIds
Gets players holding the item provided by an ID for this instance. Must either be a string, or an object.

Defaults to an inefficient map of data returned from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databasePlayersByItemIds.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databasePlayersByItemIds.type = "live"
Returns data from Blaseball, time agnostic.

### databasePlayersByItemIds.type = "static"
Returns a static set of data, parsed from `databasePlayersByItemIds.data`.

Example:
```json
{
  "databasePlayersByItemIds": {
    "type": "static",
    "data": [{"id":"38a1e7ce-64ed-433d-b29c-becc4720caa1","anticapitalism":0.0866678627331002,"baseThirst":0.10237926985001013,"buoyancy":0.6022793194801744,"chasiness":0.5122617433195847,"coldness":1.0583041307016892,"continuation":0.7692722227151576,"divinity":1.8453709796803361,"groundFriction":0.49456495791182853,"indulgence":0.7991810271247187,"laserlikeness":0.3083274291250653,"martyrdom":0.31609637356221354,"moxie":0.7307417915082235,"musclitude":1.535749789064464,"name":"Parker MacMillan","omniscience":0.27551679016879876,"overpowerment":0.12165592881414125,"patheticism":0.8609999999999999,"ruthlessness":2.012628831803399,"shakespearianism":1.965822935489618,"suppression":0.14100000000000001,"tenaciousness":0.5828231258667219,"thwackability":1.4795201120377561,"tragicness":0.8609999999999999,"unthwackability":0.18611476550937,"watchfulness":0.16517359815256782,"pressurization":0.23393556008324898,"totalFingers":10,"soul":3,"deceased":false,"peanutAllergy":false,"cinnamon":0.057040746496574035,"fate":3,"ritual":"Obsessing over past mistakes","coffee":0,"blood":10,"permAttr":["PROFIT","NONPROFIT","FIREWALKER","NEWADVENTURE","SUPERWANDERER","LEGENDARY"],"seasAttr":[],"weekAttr":[],"gameAttr":[],"hitStreak":0,"consecutiveHits":0,"baserunningRating":0.3924841648450664,"pitchingRating":0.4457954017590105,"hittingRating":1.2445295271736567,"defenseRating":0.4242931625046796,"leagueTeamId":"979aee4a-6d80-4863-bf1c-ee1a78e06024","tournamentTeamId":null,"eDensity":0,"state":{"itemModSources":{"SUPERWANDERER":["eecc9bf3-96b5-4ea9-9a4a-05f0a0d586f0"]}},"evolution":0,"items":["eecc9bf3-96b5-4ea9-9a4a-05f0a0d586f0"],"itemAttr":["SUPERWANDERER"]}]
  }
}
```

## databasePlayoffs
Gets the set of playoffs for this instance. Must either be a string, or an object.

Note that this is unused since we got the Underbracket, since it only returns **one** bracket.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databasePlayoffs.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databasePlayoffs.type = "live"
Returns data from Blaseball, time agnostic.

### databasePlayoffs.type = "static"
Returns a static set of data, parsed from `databasePlayoffs.data`.

Example:
```json
{
  "databasePlayoffs": {
    "type": "static",
    "data": {
      "id": "fc97a06c-7f99-4d83-a577-526e27d6b154",
      "name": "Overbracket 23",
      "numberOfRounds": 4,
      "playoffDay": 17,
      "rounds": [
        "2f53444d-590a-43e2-9ea6-26984bc32aed",
        "b625feec-61dc-4672-bcbe-4b0bfa27c887",
        "1be5600e-1077-4089-b930-fe702c94f19d",
        "84388a75-2f62-41cd-8b60-206a27e8c706"
      ],
      "season": 22,
      "tomorrowRound": 3,
      "winner": "eb67ae5e-c4bf-46ca-bbbc-425cd34182ff",
      "tournament": -1,
      "round": 3,
      "bracket": 0
    }
  }
}
```

## databaseRenovationProgress
Gets renovation progress for the provided stadium for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseRenovationProgress.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseRenovationProgress.type = "live"
Returns data from Blaseball, time agnostic.

### databaseRenovationProgress.type = "static"
Returns a static set of data, parsed from `databaseRenovationProgress.data`.

Example:
```json
{
  "databaseRenovationProgress": {
    "type": "static",
    "data": {
      "stats": [],
      "progress": {
        "total": 0,
        "toNext": 0
      }
    }
  }
}
```

## databaseRenovations
Gets **all** divisions for this instance. Must either be a string, or an object.

Defaults to returning data from Blaseball.

When it's a string, must be "live".
When it's an object, it requires a property to define an endpoint.

### databaseRenovations.type = "live"
Returns data from Blaseball, time agnostic.

### databaseRenovations.type = "static"
Returns a static set of data, parsed from `databaseRenovations.data`.

Example:
```json
{
  "databaseRenovations": {
    "type": "static",
    "data": [
      {
        "id": "anti_graphene_mod",
        "title": "Antigraphene",
        "type": 1,
        "effects": [
          "Modification"
        ],
        "description": "Rebuild your Ballpark's Fortifications with Antigraphene, to make them far more dense.",
        "data": {
          "mod": "ANTIGRAPHENE",
          "addMsg": "The {tnn} rebuilt the Fortifications of {snn} with Antigraphene.",
          "rmvMsg": "The {tnn} rebuilt the Fortifications of {snn} without Antigraphene."
        }
      }
    ]
  }
}
```

## databaseShopSetup
Gets shop setup details for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseShopSetup.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseShopSetup.type = "live"
Returns data from Blaseball, time agnostic.

### databaseShopSetup.type = "static"
Returns a static set of data, parsed from `databaseShopSetup.data`.

Example:
```json
{
  "databaseShopSetup": {
    "type": "static",
    "data": {"menu":["Ad","Votes","Flutes","Beg","Peanuts","Max_Bet","Team_Win","Idol_Strikeouts","Idol_Shutouts","Idol_Homers","Idol_Hits","Team_Loss","Stadium_Access","Wills_Access","Forbidden_Knowledge_Access","Idol_Steal","Black_Hole","Team_Slush","Sun_2","Idol_Homer_Allowed","Breakfast","Incineration","Idol_Pitcher_Win","Idol_Pitcher_Loss","Red_Herring"],"snackData":{"maxBetTiers":[{"price":0,"amount":20},{"price":20,"amount":40},{"price":40,"amount":60},{"price":65,"amount":80},{"price":90,"amount":100},{"price":125,"amount":120},{"price":160,"amount":140},{"price":200,"amount":160},{"price":245,"amount":180},{"price":290,"amount":200},{"price":335,"amount":220},{"price":385,"amount":240},{"price":440,"amount":260},{"price":495,"amount":280},{"price":550,"amount":300},{"price":610,"amount":320},{"price":670,"amount":340},{"price":730,"amount":360},{"price":795,"amount":380},{"price":865,"amount":400},{"price":930,"amount":420},{"price":1000,"amount":440},{"price":1075,"amount":460},{"price":1145,"amount":480},{"price":1220,"amount":500},{"price":1295,"amount":520},{"price":1375,"amount":540},{"price":1455,"amount":560},{"price":1535,"amount":580},{"price":1620,"amount":600},{"price":1700,"amount":620},{"price":1785,"amount":640},{"price":1875,"amount":660},{"price":1960,"amount":680},{"price":2050,"amount":700},{"price":2140,"amount":720},{"price":2235,"amount":740},{"price":2330,"amount":760},{"price":2420,"amount":780},{"price":2520,"amount":800},{"price":2615,"amount":820},{"price":2715,"amount":840},{"price":2815,"amount":860},{"price":2915,"amount":880},{"price":3015,"amount":900},{"price":3120,"amount":920},{"price":3225,"amount":940},{"price":3330,"amount":960},{"price":3435,"amount":980},{"price":3540,"amount":1020},{"price":3650,"amount":1040},{"price":3760,"amount":1060},{"price":3870,"amount":1080},{"price":3985,"amount":1100},{"price":4095,"amount":1120},{"price":4210,"amount":1140},{"price":4325,"amount":1160},{"price":4440,"amount":1180},{"price":4560,"amount":1200},{"price":4675,"amount":1220},{"price":4795,"amount":1240},{"price":4915,"amount":1260},{"price":5035,"amount":1280},{"price":5160,"amount":1300},{"price":5280,"amount":1320},{"price":5405,"amount":1340},{"price":5530,"amount":1360},{"price":5655,"amount":1380},{"price":5785,"amount":1400},{"price":5910,"amount":1420},{"price":6040,"amount":1440},{"price":6170,"amount":1460},{"price":6300,"amount":1480},{"price":6435,"amount":1500},{"price":6565,"amount":1520},{"price":6700,"amount":1540},{"price":6835,"amount":1560},{"price":6970,"amount":1580},{"price":7105,"amount":1600},{"price":7240,"amount":1620},{"price":7380,"amount":1640},{"price":7515,"amount":1660},{"price":7655,"amount":1680},{"price":7795,"amount":1700},{"price":7940,"amount":1720},{"price":8080,"amount":1740},{"price":8220,"amount":1760},{"price":8365,"amount":1780},{"price":8510,"amount":1800},{"price":8655,"amount":1820},{"price":8800,"amount":1840},{"price":8950,"amount":1860},{"price":9095,"amount":1880},{"price":9245,"amount":1900},{"price":9395,"amount":1920},{"price":9545,"amount":1940},{"price":9695,"amount":1960},{"price":9845,"amount":1980},{"price":10000,"amount":2000}],"teamWinCoinTiers":[{"price":10,"amount":10},{"price":10,"amount":12},{"price":10,"amount":16},{"price":10,"amount":18},{"price":10,"amount":22},{"price":15,"amount":24},{"price":15,"amount":28},{"price":15,"amount":30},{"price":15,"amount":34},{"price":20,"amount":36},{"price":20,"amount":40},{"price":20,"amount":42},{"price":25,"amount":46},{"price":25,"amount":48},{"price":30,"amount":52},{"price":35,"amount":54},{"price":35,"amount":58},{"price":40,"amount":60},{"price":45,"amount":64},{"price":45,"amount":66},{"price":50,"amount":70},{"price":55,"amount":72},{"price":60,"amount":76},{"price":65,"amount":78},{"price":70,"amount":82},{"price":75,"amount":84},{"price":80,"amount":86},{"price":85,"amount":90},{"price":90,"amount":92},{"price":95,"amount":96},{"price":105,"amount":98},{"price":110,"amount":102},{"price":115,"amount":104},{"price":120,"amount":108},{"price":130,"amount":110},{"price":135,"amount":114},{"price":145,"amount":116},{"price":150,"amount":120},{"price":160,"amount":122},{"price":165,"amount":126},{"price":175,"amount":128},{"price":185,"amount":132},{"price":190,"amount":134},{"price":200,"amount":138},{"price":210,"amount":140},{"price":220,"amount":144},{"price":230,"amount":146},{"price":240,"amount":150},{"price":250,"amount":152},{"price":260,"amount":156},{"price":270,"amount":158},{"price":280,"amount":160},{"price":290,"amount":164},{"price":300,"amount":166},{"price":310,"amount":170},{"price":320,"amount":172},{"price":335,"amount":176},{"price":345,"amount":178},{"price":355,"amount":182},{"price":370,"amount":184},{"price":380,"amount":188},{"price":395,"amount":190},{"price":405,"amount":194},{"price":420,"amount":196},{"price":430,"amount":200},{"price":445,"amount":202},{"price":460,"amount":206},{"price":475,"amount":208},{"price":485,"amount":212},{"price":500,"amount":214},{"price":515,"amount":218},{"price":530,"amount":220},{"price":545,"amount":224},{"price":560,"amount":226},{"price":575,"amount":228},{"price":590,"amount":232},{"price":605,"amount":234},{"price":620,"amount":238},{"price":635,"amount":240},{"price":655,"amount":244},{"price":670,"amount":246},{"price":685,"amount":250},{"price":705,"amount":252},{"price":720,"amount":256},{"price":735,"amount":258},{"price":755,"amount":262},{"price":770,"amount":264},{"price":790,"amount":268},{"price":810,"amount":270},{"price":825,"amount":274},{"price":845,"amount":276},{"price":865,"amount":280},{"price":880,"amount":282},{"price":900,"amount":286},{"price":920,"amount":288},{"price":940,"amount":292},{"price":960,"amount":294},{"price":980,"amount":298},{"price":1000,"amount":300}],"idolHitsTiers":[{"price":25,"amount":5},{"price":35,"amount":11},{"price":55,"amount":19},{"price":80,"amount":28},{"price":105,"amount":37},{"price":140,"amount":47},{"price":175,"amount":57},{"price":215,"amount":68},{"price":260,"amount":79},{"price":305,"amount":90},{"price":350,"amount":102},{"price":400,"amount":113},{"price":450,"amount":125},{"price":505,"amount":137},{"price":565,"amount":150},{"price":620,"amount":162},{"price":685,"amount":175},{"price":745,"amount":188},{"price":810,"amount":201},{"price":875,"amount":214},{"price":945,"amount":227},{"price":1015,"amount":240},{"price":1085,"amount":254},{"price":1160,"amount":268},{"price":1235,"amount":281},{"price":1310,"amount":295},{"price":1390,"amount":309},{"price":1470,"amount":323},{"price":1550,"amount":337},{"price":1630,"amount":352},{"price":1715,"amount":366},{"price":1800,"amount":381},{"price":1885,"amount":395},{"price":1975,"amount":410},{"price":2065,"amount":425},{"price":2155,"amount":440},{"price":2245,"amount":455},{"price":2340,"amount":470},{"price":2435,"amount":485},{"price":2530,"amount":500},{"price":2625,"amount":515},{"price":2725,"amount":530},{"price":2825,"amount":546},{"price":2925,"amount":561},{"price":3025,"amount":577},{"price":3130,"amount":593},{"price":3235,"amount":608},{"price":3340,"amount":624},{"price":3445,"amount":640},{"price":3550,"amount":656},{"price":3660,"amount":672},{"price":3770,"amount":688},{"price":3880,"amount":704},{"price":3990,"amount":720},{"price":4105,"amount":736},{"price":4220,"amount":752},{"price":4335,"amount":769},{"price":4450,"amount":785},{"price":4565,"amount":802},{"price":4685,"amount":818},{"price":4805,"amount":835},{"price":4925,"amount":851},{"price":5045,"amount":868},{"price":5165,"amount":885},{"price":5290,"amount":902},{"price":5415,"amount":918},{"price":5540,"amount":935},{"price":5665,"amount":952},{"price":5790,"amount":969},{"price":5920,"amount":986},{"price":6045,"amount":1003},{"price":6175,"amount":1020},{"price":6305,"amount":1038},{"price":6440,"amount":1055},{"price":6570,"amount":1072},{"price":6705,"amount":1090},{"price":6835,"amount":1107},{"price":6970,"amount":1124},{"price":7110,"amount":1142},{"price":7245,"amount":1159},{"price":7380,"amount":1177},{"price":7520,"amount":1194},{"price":7660,"amount":1212},{"price":7800,"amount":1230},{"price":7940,"amount":1248},{"price":8085,"amount":1265},{"price":8225,"amount":1283},{"price":8370,"amount":1301},{"price":8515,"amount":1319},{"price":8660,"amount":1337},{"price":8805,"amount":1355},{"price":8950,"amount":1373},{"price":9100,"amount":1391},{"price":9245,"amount":1409},{"price":9395,"amount":1427},{"price":9545,"amount":1445},{"price":9695,"amount":1463},{"price":9850,"amount":1482},{"price":10000,"amount":1500}],"idolHomersTiers":[{"price":20,"amount":20},{"price":35,"amount":60},{"price":65,"amount":100},{"price":95,"amount":140},{"price":135,"amount":180},{"price":175,"amount":225},{"price":220,"amount":265},{"price":270,"amount":305},{"price":320,"amount":345},{"price":375,"amount":385},{"price":430,"amount":425},{"price":485,"amount":465},{"price":550,"amount":505},{"price":610,"amount":550},{"price":675,"amount":590},{"price":740,"amount":630},{"price":810,"amount":670},{"price":880,"amount":710},{"price":950,"amount":750},{"price":1025,"amount":790},{"price":1100,"amount":830},{"price":1175,"amount":875},{"price":1255,"amount":915},{"price":1330,"amount":955},{"price":1410,"amount":995},{"price":1495,"amount":1035},{"price":1575,"amount":1075},{"price":1660,"amount":1115},{"price":1750,"amount":1155},{"price":1835,"amount":1200},{"price":1925,"amount":1240},{"price":2010,"amount":1280},{"price":2105,"amount":1320},{"price":2195,"amount":1360},{"price":2285,"amount":1400},{"price":2380,"amount":1440},{"price":2475,"amount":1480},{"price":2570,"amount":1525},{"price":2670,"amount":1565},{"price":2765,"amount":1605},{"price":2865,"amount":1645},{"price":2965,"amount":1685},{"price":3070,"amount":1725},{"price":3170,"amount":1765},{"price":3275,"amount":1805},{"price":3375,"amount":1850},{"price":3480,"amount":1890},{"price":3585,"amount":1930},{"price":3695,"amount":1970},{"price":3800,"amount":2010},{"price":3910,"amount":2050},{"price":4020,"amount":2090},{"price":4130,"amount":2130},{"price":4240,"amount":2170},{"price":4355,"amount":2215},{"price":4465,"amount":2255},{"price":4580,"amount":2295},{"price":4695,"amount":2335},{"price":4810,"amount":2375},{"price":4925,"amount":2415},{"price":5040,"amount":2455},{"price":5160,"amount":2495},{"price":5275,"amount":2540},{"price":5395,"amount":2580},{"price":5515,"amount":2620},{"price":5635,"amount":2660},{"price":5760,"amount":2700},{"price":5880,"amount":2740},{"price":6005,"amount":2780},{"price":6125,"amount":2820},{"price":6250,"amount":2865},{"price":6375,"amount":2905},{"price":6500,"amount":2945},{"price":6630,"amount":2985},{"price":6755,"amount":3025},{"price":6885,"amount":3065},{"price":7010,"amount":3105},{"price":7140,"amount":3145},{"price":7270,"amount":3190},{"price":7400,"amount":3230},{"price":7530,"amount":3270},{"price":7665,"amount":3310},{"price":7795,"amount":3350},{"price":7930,"amount":3390},{"price":8065,"amount":3430},{"price":8195,"amount":3470},{"price":8330,"amount":3515},{"price":8470,"amount":3555},{"price":8605,"amount":3595},{"price":8740,"amount":3635},{"price":8880,"amount":3675},{"price":9015,"amount":3715},{"price":9155,"amount":3755},{"price":9295,"amount":3795},{"price":9435,"amount":3840},{"price":9575,"amount":3880},{"price":9715,"amount":3920},{"price":9860,"amount":3960},{"price":10000,"amount":4000}],"idolStrikeoutsTiers":[{"price":25,"amount":3},{"price":35,"amount":5},{"price":55,"amount":7},{"price":80,"amount":9},{"price":105,"amount":11},{"price":135,"amount":13},{"price":165,"amount":15},{"price":195,"amount":17},{"price":230,"amount":19},{"price":265,"amount":21},{"price":300,"amount":23},{"price":335,"amount":25},{"price":375,"amount":27},{"price":415,"amount":29},{"price":455,"amount":31},{"price":500,"amount":33},{"price":540,"amount":35},{"price":585,"amount":37},{"price":630,"amount":39},{"price":675,"amount":41},{"price":725,"amount":43},{"price":770,"amount":45},{"price":820,"amount":47},{"price":870,"amount":49},{"price":920,"amount":51},{"price":970,"amount":53},{"price":1020,"amount":55},{"price":1075,"amount":57},{"price":1125,"amount":59},{"price":1180,"amount":61},{"price":1235,"amount":63},{"price":1290,"amount":65},{"price":1345,"amount":67},{"price":1400,"amount":69},{"price":1455,"amount":71},{"price":1515,"amount":73},{"price":1570,"amount":75},{"price":1630,"amount":77},{"price":1690,"amount":79},{"price":1745,"amount":81},{"price":1805,"amount":83},{"price":1870,"amount":85},{"price":1930,"amount":87},{"price":1990,"amount":89},{"price":2050,"amount":91},{"price":2115,"amount":93},{"price":2175,"amount":95},{"price":2240,"amount":97},{"price":2305,"amount":99},{"price":2370,"amount":102},{"price":2435,"amount":104},{"price":2500,"amount":106},{"price":2565,"amount":108},{"price":2630,"amount":110},{"price":2695,"amount":112},{"price":2765,"amount":114},{"price":2830,"amount":116},{"price":2900,"amount":118},{"price":2970,"amount":120},{"price":3035,"amount":122},{"price":3105,"amount":124},{"price":3175,"amount":126},{"price":3245,"amount":128},{"price":3315,"amount":130},{"price":3385,"amount":132},{"price":3460,"amount":134},{"price":3530,"amount":136},{"price":3600,"amount":138},{"price":3675,"amount":140},{"price":3745,"amount":142},{"price":3820,"amount":144},{"price":3890,"amount":146},{"price":3965,"amount":148},{"price":4040,"amount":150},{"price":4115,"amount":152},{"price":4190,"amount":154},{"price":4265,"amount":156},{"price":4340,"amount":158},{"price":4415,"amount":160},{"price":4490,"amount":162},{"price":4570,"amount":164},{"price":4645,"amount":166},{"price":4720,"amount":168},{"price":4800,"amount":170},{"price":4875,"amount":172},{"price":4955,"amount":174},{"price":5035,"amount":176},{"price":5115,"amount":178},{"price":5190,"amount":180},{"price":5270,"amount":182},{"price":5350,"amount":184},{"price":5430,"amount":186},{"price":5510,"amount":188},{"price":5590,"amount":190},{"price":5675,"amount":192},{"price":5755,"amount":194},{"price":5835,"amount":196},{"price":5920,"amount":198},{"price":6000,"amount":200}],"idolShutoutsTiers":[{"price":100,"amount":250},{"price":120,"amount":380},{"price":140,"amount":500},{"price":180,"amount":630},{"price":220,"amount":750},{"price":270,"amount":880},{"price":330,"amount":1000},{"price":380,"amount":1130},{"price":450,"amount":1250},{"price":510,"amount":1380},{"price":590,"amount":1500},{"price":660,"amount":1630},{"price":740,"amount":1750},{"price":820,"amount":1880},{"price":900,"amount":2000},{"price":990,"amount":2130},{"price":1080,"amount":2250},{"price":1180,"amount":2380},{"price":1270,"amount":2500},{"price":1370,"amount":2630},{"price":1470,"amount":2750},{"price":1580,"amount":2880},{"price":1680,"amount":3000},{"price":1790,"amount":3130},{"price":1910,"amount":3250},{"price":2020,"amount":3380},{"price":2140,"amount":3500},{"price":2250,"amount":3620},{"price":2380,"amount":3750},{"price":2500,"amount":3880},{"price":2620,"amount":4000},{"price":2750,"amount":4130},{"price":2880,"amount":4250},{"price":3010,"amount":4380},{"price":3140,"amount":4500},{"price":3280,"amount":4630},{"price":3420,"amount":4750},{"price":3560,"amount":4880},{"price":3700,"amount":5000},{"price":3840,"amount":5130},{"price":3990,"amount":5250},{"price":4130,"amount":5380},{"price":4280,"amount":5500},{"price":4430,"amount":5630},{"price":4580,"amount":5750},{"price":4740,"amount":5880},{"price":4890,"amount":6000},{"price":5050,"amount":6130},{"price":5210,"amount":6250},{"price":5370,"amount":6380},{"price":5530,"amount":6500},{"price":5690,"amount":6630},{"price":5860,"amount":6750},{"price":6030,"amount":6880},{"price":6190,"amount":7000},{"price":6360,"amount":7120},{"price":6540,"amount":7250},{"price":6710,"amount":7380},{"price":6880,"amount":7500},{"price":7060,"amount":7630},{"price":7240,"amount":7750},{"price":7420,"amount":7880},{"price":7600,"amount":8000},{"price":7780,"amount":8130},{"price":7960,"amount":8250},{"price":8150,"amount":8380},{"price":8330,"amount":8500},{"price":8520,"amount":8630},{"price":8710,"amount":8750},{"price":8900,"amount":8880},{"price":9090,"amount":9000},{"price":9290,"amount":9130},{"price":9480,"amount":9250},{"price":9680,"amount":9380},{"price":9880,"amount":9500},{"price":10080,"amount":9630},{"price":10280,"amount":9750},{"price":10480,"amount":9880},{"price":10680,"amount":10000},{"price":10880,"amount":10130},{"price":11090,"amount":10250},{"price":11300,"amount":10380},{"price":11500,"amount":10500},{"price":11710,"amount":10630},{"price":11920,"amount":10750},{"price":12140,"amount":10880},{"price":12350,"amount":11000},{"price":12560,"amount":11130},{"price":12780,"amount":11250},{"price":13000,"amount":11380},{"price":13210,"amount":11500},{"price":13430,"amount":11630},{"price":13650,"amount":11750},{"price":13870,"amount":11880},{"price":14100,"amount":12000},{"price":14320,"amount":12130},{"price":14550,"amount":12250},{"price":14770,"amount":12380},{"price":15000,"amount":12500}],"teamLossCoinTiers":[{"price":10,"amount":10},{"price":10,"amount":20},{"price":10,"amount":25},{"price":10,"amount":35},{"price":15,"amount":40},{"price":15,"amount":50},{"price":15,"amount":60},{"price":20,"amount":65},{"price":25,"amount":75},{"price":25,"amount":85},{"price":30,"amount":90},{"price":35,"amount":100},{"price":40,"amount":105},{"price":45,"amount":115},{"price":50,"amount":125},{"price":55,"amount":130},{"price":65,"amount":140},{"price":70,"amount":145},{"price":75,"amount":155},{"price":85,"amount":165},{"price":95,"amount":170},{"price":100,"amount":180},{"price":110,"amount":185},{"price":120,"amount":195},{"price":130,"amount":205},{"price":140,"amount":210},{"price":150,"amount":220},{"price":160,"amount":230},{"price":170,"amount":235},{"price":185,"amount":245},{"price":195,"amount":250},{"price":210,"amount":260},{"price":220,"amount":270},{"price":235,"amount":275},{"price":250,"amount":285},{"price":265,"amount":290},{"price":280,"amount":300},{"price":295,"amount":310},{"price":310,"amount":315},{"price":325,"amount":325},{"price":340,"amount":330},{"price":360,"amount":340},{"price":375,"amount":350},{"price":395,"amount":355},{"price":410,"amount":365},{"price":430,"amount":375},{"price":450,"amount":380},{"price":470,"amount":390},{"price":485,"amount":395},{"price":510,"amount":405},{"price":530,"amount":415},{"price":550,"amount":420},{"price":570,"amount":430},{"price":590,"amount":435},{"price":615,"amount":445},{"price":635,"amount":455},{"price":660,"amount":460},{"price":685,"amount":470},{"price":705,"amount":480},{"price":730,"amount":485},{"price":755,"amount":495},{"price":780,"amount":500},{"price":805,"amount":510},{"price":830,"amount":520},{"price":860,"amount":525},{"price":885,"amount":535},{"price":915,"amount":540},{"price":940,"amount":550},{"price":970,"amount":560},{"price":995,"amount":565},{"price":1025,"amount":575},{"price":1055,"amount":580},{"price":1085,"amount":590},{"price":1115,"amount":600},{"price":1145,"amount":605},{"price":1175,"amount":615},{"price":1205,"amount":625},{"price":1240,"amount":630},{"price":1270,"amount":640},{"price":1305,"amount":645},{"price":1335,"amount":655},{"price":1370,"amount":665},{"price":1405,"amount":670},{"price":1435,"amount":680},{"price":1470,"amount":685},{"price":1505,"amount":695},{"price":1540,"amount":705},{"price":1580,"amount":710},{"price":1615,"amount":720},{"price":1650,"amount":725},{"price":1690,"amount":735},{"price":1725,"amount":745},{"price":1765,"amount":750},{"price":1800,"amount":760},{"price":1840,"amount":770},{"price":1880,"amount":775},{"price":1920,"amount":785},{"price":1960,"amount":790},{"price":2000,"amount":800}],"idolStealTiers":[{"price":10,"amount":50},{"price":20,"amount":80},{"price":35,"amount":110},{"price":60,"amount":140},{"price":85,"amount":170},{"price":115,"amount":200},{"price":145,"amount":230},{"price":180,"amount":260},{"price":220,"amount":290},{"price":260,"amount":320},{"price":305,"amount":350},{"price":350,"amount":380},{"price":395,"amount":410},{"price":445,"amount":440},{"price":495,"amount":470},{"price":550,"amount":500},{"price":605,"amount":530},{"price":660,"amount":560},{"price":720,"amount":590},{"price":775,"amount":620},{"price":840,"amount":650},{"price":900,"amount":680},{"price":965,"amount":710},{"price":1030,"amount":740},{"price":1100,"amount":770},{"price":1170,"amount":805},{"price":1240,"amount":835},{"price":1310,"amount":865},{"price":1385,"amount":895},{"price":1455,"amount":925},{"price":1535,"amount":955},{"price":1610,"amount":985},{"price":1685,"amount":1015},{"price":1765,"amount":1045},{"price":1845,"amount":1075},{"price":1930,"amount":1105},{"price":2010,"amount":1135},{"price":2095,"amount":1165},{"price":2180,"amount":1195},{"price":2265,"amount":1225},{"price":2355,"amount":1255},{"price":2445,"amount":1285},{"price":2530,"amount":1315},{"price":2625,"amount":1345},{"price":2715,"amount":1375},{"price":2805,"amount":1405},{"price":2900,"amount":1435},{"price":2995,"amount":1465},{"price":3090,"amount":1495},{"price":3190,"amount":1525},{"price":3285,"amount":1555},{"price":3385,"amount":1585},{"price":3485,"amount":1615},{"price":3585,"amount":1645},{"price":3685,"amount":1675},{"price":3790,"amount":1705},{"price":3895,"amount":1735},{"price":4000,"amount":1765},{"price":4105,"amount":1795},{"price":4210,"amount":1825},{"price":4315,"amount":1855},{"price":4425,"amount":1885},{"price":4535,"amount":1915},{"price":4645,"amount":1945},{"price":4755,"amount":1975},{"price":4865,"amount":2005},{"price":4980,"amount":2035},{"price":5090,"amount":2065},{"price":5205,"amount":2095},{"price":5320,"amount":2125},{"price":5435,"amount":2155},{"price":5555,"amount":2185},{"price":5670,"amount":2215},{"price":5790,"amount":2245},{"price":5910,"amount":2280},{"price":6030,"amount":2310},{"price":6150,"amount":2340},{"price":6270,"amount":2370},{"price":6395,"amount":2400},{"price":6515,"amount":2430},{"price":6640,"amount":2460},{"price":6765,"amount":2490},{"price":6890,"amount":2520},{"price":7015,"amount":2550},{"price":7145,"amount":2580},{"price":7270,"amount":2610},{"price":7400,"amount":2640},{"price":7530,"amount":2670},{"price":7660,"amount":2700},{"price":7790,"amount":2730},{"price":7920,"amount":2760},{"price":8055,"amount":2790},{"price":8185,"amount":2820},{"price":8320,"amount":2850},{"price":8455,"amount":2880},{"price":8590,"amount":2910},{"price":8725,"amount":2940},{"price":8865,"amount":2970},{"price":9000,"amount":3000}],"blackHoleTiers":[{"price":100,"amount":100},{"price":265,"amount":250},{"price":475,"amount":405},{"price":710,"amount":555},{"price":960,"amount":710},{"price":1225,"amount":860},{"price":1495,"amount":1010},{"price":1780,"amount":1165},{"price":2075,"amount":1315},{"price":2375,"amount":1470},{"price":2680,"amount":1620},{"price":2990,"amount":1770},{"price":3310,"amount":1925},{"price":3635,"amount":2075},{"price":3960,"amount":2230},{"price":4295,"amount":2380},{"price":4635,"amount":2535},{"price":4975,"amount":2685},{"price":5320,"amount":2835},{"price":5670,"amount":2990},{"price":6025,"amount":3140},{"price":6385,"amount":3295},{"price":6745,"amount":3445},{"price":7110,"amount":3595},{"price":7475,"amount":3750},{"price":7845,"amount":3900},{"price":8220,"amount":4055},{"price":8595,"amount":4205},{"price":8975,"amount":4355},{"price":9355,"amount":4510},{"price":9740,"amount":4660},{"price":10125,"amount":4815},{"price":10515,"amount":4965},{"price":10905,"amount":5115},{"price":11300,"amount":5270},{"price":11700,"amount":5420},{"price":12095,"amount":5575},{"price":12500,"amount":5725},{"price":12900,"amount":5880},{"price":13305,"amount":6030},{"price":13715,"amount":6180},{"price":14125,"amount":6335},{"price":14535,"amount":6485},{"price":14950,"amount":6640},{"price":15365,"amount":6790},{"price":15780,"amount":6940},{"price":16200,"amount":7095},{"price":16620,"amount":7245},{"price":17045,"amount":7400},{"price":17465,"amount":7550},{"price":17895,"amount":7700},{"price":18320,"amount":7855},{"price":18750,"amount":8005},{"price":19180,"amount":8160},{"price":19615,"amount":8310},{"price":20050,"amount":8460},{"price":20485,"amount":8615},{"price":20925,"amount":8765},{"price":21365,"amount":8920},{"price":21805,"amount":9070},{"price":22245,"amount":9220},{"price":22690,"amount":9375},{"price":23135,"amount":9525},{"price":23580,"amount":9680},{"price":24030,"amount":9830},{"price":24480,"amount":9985},{"price":24930,"amount":10135},{"price":25380,"amount":10285},{"price":25835,"amount":10440},{"price":26290,"amount":10590},{"price":26745,"amount":10745},{"price":27205,"amount":10895},{"price":27660,"amount":11045},{"price":28120,"amount":11200},{"price":28585,"amount":11350},{"price":29045,"amount":11505},{"price":29510,"amount":11655},{"price":29975,"amount":11805},{"price":30440,"amount":11960},{"price":30905,"amount":12110},{"price":31375,"amount":12265},{"price":31845,"amount":12415},{"price":32315,"amount":12565},{"price":32790,"amount":12720},{"price":33260,"amount":12870},{"price":33735,"amount":13025},{"price":34210,"amount":13175},{"price":34690,"amount":13330},{"price":35165,"amount":13480},{"price":35645,"amount":13630},{"price":36125,"amount":13785},{"price":36605,"amount":13935},{"price":37085,"amount":14090},{"price":37570,"amount":14240},{"price":38055,"amount":14390},{"price":38540,"amount":14545},{"price":39025,"amount":14695},{"price":39510,"amount":14850},{"price":40000,"amount":15000}],"floodClearTiers":[{"price":10,"amount":10},{"price":10,"amount":15},{"price":15,"amount":20},{"price":20,"amount":25},{"price":30,"amount":30},{"price":35,"amount":35},{"price":45,"amount":40},{"price":55,"amount":45},{"price":65,"amount":50},{"price":75,"amount":55},{"price":90,"amount":60},{"price":100,"amount":65},{"price":115,"amount":70},{"price":130,"amount":75},{"price":145,"amount":80},{"price":160,"amount":85},{"price":175,"amount":90},{"price":190,"amount":95},{"price":210,"amount":100},{"price":225,"amount":105},{"price":245,"amount":110},{"price":265,"amount":115},{"price":285,"amount":120},{"price":305,"amount":125},{"price":325,"amount":130},{"price":345,"amount":135},{"price":370,"amount":140},{"price":390,"amount":145},{"price":415,"amount":150},{"price":435,"amount":155},{"price":460,"amount":160},{"price":485,"amount":165},{"price":510,"amount":170},{"price":535,"amount":175},{"price":560,"amount":180},{"price":585,"amount":185},{"price":610,"amount":190},{"price":640,"amount":195},{"price":665,"amount":200},{"price":695,"amount":205},{"price":725,"amount":210},{"price":750,"amount":215},{"price":780,"amount":220},{"price":810,"amount":225},{"price":840,"amount":230},{"price":870,"amount":235},{"price":900,"amount":240},{"price":935,"amount":245},{"price":965,"amount":250},{"price":995,"amount":255},{"price":1030,"amount":260},{"price":1060,"amount":265},{"price":1095,"amount":270},{"price":1130,"amount":275},{"price":1160,"amount":280},{"price":1195,"amount":285},{"price":1230,"amount":290},{"price":1265,"amount":295},{"price":1300,"amount":300},{"price":1340,"amount":305},{"price":1375,"amount":310},{"price":1410,"amount":315},{"price":1445,"amount":320},{"price":1485,"amount":325},{"price":1520,"amount":330},{"price":1560,"amount":335},{"price":1600,"amount":340},{"price":1635,"amount":345},{"price":1675,"amount":350},{"price":1715,"amount":355},{"price":1755,"amount":360},{"price":1795,"amount":365},{"price":1835,"amount":370},{"price":1875,"amount":375},{"price":1920,"amount":380},{"price":1960,"amount":385},{"price":2000,"amount":390},{"price":2045,"amount":395},{"price":2085,"amount":400},{"price":2130,"amount":405},{"price":2170,"amount":410},{"price":2215,"amount":415},{"price":2260,"amount":420},{"price":2300,"amount":425},{"price":2345,"amount":430},{"price":2390,"amount":435},{"price":2435,"amount":440},{"price":2480,"amount":445},{"price":2525,"amount":450},{"price":2575,"amount":455},{"price":2620,"amount":460},{"price":2665,"amount":465},{"price":2715,"amount":470},{"price":2760,"amount":475},{"price":2805,"amount":480},{"price":2855,"amount":485},{"price":2905,"amount":490},{"price":2950,"amount":495},{"price":3000,"amount":500}],"idolHomerAllowedTiers":[{"price":20,"amount":20},{"price":25,"amount":30},{"price":30,"amount":40},{"price":40,"amount":50},{"price":55,"amount":60},{"price":65,"amount":70},{"price":80,"amount":80},{"price":95,"amount":90},{"price":115,"amount":100},{"price":130,"amount":110},{"price":150,"amount":120},{"price":170,"amount":130},{"price":190,"amount":140},{"price":210,"amount":150},{"price":235,"amount":160},{"price":260,"amount":170},{"price":285,"amount":180},{"price":310,"amount":190},{"price":335,"amount":200},{"price":360,"amount":210},{"price":385,"amount":220},{"price":415,"amount":230},{"price":445,"amount":240},{"price":475,"amount":250},{"price":500,"amount":260},{"price":535,"amount":270},{"price":565,"amount":280},{"price":595,"amount":290},{"price":630,"amount":300},{"price":660,"amount":310},{"price":695,"amount":320},{"price":730,"amount":330},{"price":765,"amount":340},{"price":800,"amount":350},{"price":835,"amount":360},{"price":870,"amount":370},{"price":905,"amount":380},{"price":945,"amount":390},{"price":980,"amount":400},{"price":1020,"amount":410},{"price":1060,"amount":420},{"price":1095,"amount":430},{"price":1135,"amount":440},{"price":1175,"amount":450},{"price":1215,"amount":460},{"price":1260,"amount":470},{"price":1300,"amount":480},{"price":1340,"amount":490},{"price":1385,"amount":500},{"price":1425,"amount":510},{"price":1470,"amount":520},{"price":1515,"amount":530},{"price":1560,"amount":540},{"price":1605,"amount":550},{"price":1650,"amount":560},{"price":1695,"amount":570},{"price":1740,"amount":580},{"price":1785,"amount":590},{"price":1830,"amount":600},{"price":1880,"amount":610},{"price":1925,"amount":620},{"price":1975,"amount":630},{"price":2025,"amount":640},{"price":2070,"amount":650},{"price":2120,"amount":660},{"price":2170,"amount":670},{"price":2220,"amount":680},{"price":2270,"amount":690},{"price":2320,"amount":700},{"price":2370,"amount":710},{"price":2425,"amount":720},{"price":2475,"amount":730},{"price":2525,"amount":740},{"price":2580,"amount":750},{"price":2630,"amount":760},{"price":2685,"amount":770},{"price":2740,"amount":780},{"price":2790,"amount":790},{"price":2845,"amount":800},{"price":2900,"amount":810},{"price":2955,"amount":820},{"price":3010,"amount":830},{"price":3065,"amount":840},{"price":3120,"amount":850},{"price":3180,"amount":860},{"price":3235,"amount":870},{"price":3290,"amount":880},{"price":3350,"amount":890},{"price":3405,"amount":900},{"price":3465,"amount":910},{"price":3525,"amount":920},{"price":3580,"amount":930},{"price":3640,"amount":940},{"price":3700,"amount":950},{"price":3760,"amount":960},{"price":3820,"amount":970},{"price":3880,"amount":980},{"price":3940,"amount":990},{"price":4000,"amount":1000}],"timeOffTiers":[{"price":10,"amount":2000},{"price":40,"amount":2185},{"price":70,"amount":2365},{"price":100,"amount":2550},{"price":130,"amount":2735},{"price":165,"amount":2920},{"price":195,"amount":3100},{"price":225,"amount":3285},{"price":255,"amount":3470},{"price":285,"amount":3655},{"price":315,"amount":3835},{"price":345,"amount":4020},{"price":375,"amount":4205},{"price":405,"amount":4390},{"price":435,"amount":4570},{"price":470,"amount":4755},{"price":500,"amount":4940},{"price":530,"amount":5120},{"price":560,"amount":5305},{"price":590,"amount":5490},{"price":620,"amount":5675},{"price":650,"amount":5855},{"price":680,"amount":6040},{"price":710,"amount":6225},{"price":740,"amount":6410},{"price":775,"amount":6590},{"price":805,"amount":6775},{"price":835,"amount":6960},{"price":865,"amount":7145},{"price":895,"amount":7325},{"price":925,"amount":7510},{"price":955,"amount":7695},{"price":985,"amount":7880},{"price":1015,"amount":8060},{"price":1045,"amount":8245},{"price":1080,"amount":8430},{"price":1110,"amount":8610},{"price":1140,"amount":8795},{"price":1170,"amount":8980},{"price":1200,"amount":9165},{"price":1230,"amount":9345},{"price":1260,"amount":9530},{"price":1290,"amount":9715},{"price":1320,"amount":9900},{"price":1350,"amount":10080},{"price":1385,"amount":10265},{"price":1415,"amount":10450},{"price":1445,"amount":10635},{"price":1475,"amount":10815},{"price":1505,"amount":11000},{"price":1535,"amount":11185},{"price":1565,"amount":11365},{"price":1595,"amount":11550},{"price":1625,"amount":11735},{"price":1660,"amount":11920},{"price":1690,"amount":12100},{"price":1720,"amount":12285},{"price":1750,"amount":12470},{"price":1780,"amount":12655},{"price":1810,"amount":12835},{"price":1840,"amount":13020},{"price":1870,"amount":13205},{"price":1900,"amount":13390},{"price":1930,"amount":13570},{"price":1965,"amount":13755},{"price":1995,"amount":13940},{"price":2025,"amount":14120},{"price":2055,"amount":14305},{"price":2085,"amount":14490},{"price":2115,"amount":14675},{"price":2145,"amount":14855},{"price":2175,"amount":15040},{"price":2205,"amount":15225},{"price":2235,"amount":15410},{"price":2270,"amount":15590},{"price":2300,"amount":15775},{"price":2330,"amount":15960},{"price":2360,"amount":16145},{"price":2390,"amount":16325},{"price":2420,"amount":16510},{"price":2450,"amount":16695},{"price":2480,"amount":16880},{"price":2510,"amount":17060},{"price":2540,"amount":17245},{"price":2575,"amount":17430},{"price":2605,"amount":17610},{"price":2635,"amount":17795},{"price":2665,"amount":17980},{"price":2695,"amount":18165},{"price":2725,"amount":18345},{"price":2755,"amount":18530},{"price":2785,"amount":18715},{"price":2815,"amount":18900},{"price":2845,"amount":19080},{"price":2880,"amount":19265},{"price":2910,"amount":19450},{"price":2940,"amount":19635},{"price":2970,"amount":19815},{"price":3000,"amount":20000}],"sunTwoTiers":[{"price":100,"amount":100},{"price":200,"amount":200},{"price":335,"amount":300},{"price":480,"amount":405},{"price":635,"amount":505},{"price":800,"amount":605},{"price":970,"amount":705},{"price":1150,"amount":805},{"price":1330,"amount":910},{"price":1520,"amount":1010},{"price":1710,"amount":1110},{"price":1905,"amount":1210},{"price":2105,"amount":1310},{"price":2305,"amount":1415},{"price":2510,"amount":1515},{"price":2720,"amount":1615},{"price":2930,"amount":1715},{"price":3145,"amount":1815},{"price":3360,"amount":1920},{"price":3575,"amount":2020},{"price":3800,"amount":2120},{"price":4020,"amount":2220},{"price":4245,"amount":2320},{"price":4475,"amount":2425},{"price":4700,"amount":2525},{"price":4935,"amount":2625},{"price":5165,"amount":2725},{"price":5400,"amount":2830},{"price":5640,"amount":2930},{"price":5875,"amount":3030},{"price":6115,"amount":3130},{"price":6355,"amount":3230},{"price":6600,"amount":3335},{"price":6845,"amount":3435},{"price":7090,"amount":3535},{"price":7340,"amount":3635},{"price":7585,"amount":3735},{"price":7835,"amount":3840},{"price":8090,"amount":3940},{"price":8340,"amount":4040},{"price":8595,"amount":4140},{"price":8850,"amount":4240},{"price":9110,"amount":4345},{"price":9365,"amount":4445},{"price":9625,"amount":4545},{"price":9885,"amount":4645},{"price":10145,"amount":4745},{"price":10410,"amount":4850},{"price":10675,"amount":4950},{"price":10940,"amount":5050},{"price":11205,"amount":5150},{"price":11470,"amount":5250},{"price":11740,"amount":5355},{"price":12010,"amount":5455},{"price":12280,"amount":5555},{"price":12550,"amount":5655},{"price":12820,"amount":5755},{"price":13095,"amount":5860},{"price":13370,"amount":5960},{"price":13645,"amount":6060},{"price":13920,"amount":6160},{"price":14195,"amount":6260},{"price":14475,"amount":6365},{"price":14755,"amount":6465},{"price":15035,"amount":6565},{"price":15315,"amount":6665},{"price":15595,"amount":6765},{"price":15875,"amount":6870},{"price":16160,"amount":6970},{"price":16445,"amount":7070},{"price":16730,"amount":7170},{"price":17015,"amount":7270},{"price":17300,"amount":7375},{"price":17585,"amount":7475},{"price":17875,"amount":7575},{"price":18165,"amount":7675},{"price":18455,"amount":7780},{"price":18745,"amount":7880},{"price":19035,"amount":7980},{"price":19325,"amount":8080},{"price":19620,"amount":8180},{"price":19910,"amount":8285},{"price":20205,"amount":8385},{"price":20500,"amount":8485},{"price":20795,"amount":8585},{"price":21090,"amount":8685},{"price":21390,"amount":8790},{"price":21685,"amount":8890},{"price":21985,"amount":8990},{"price":22280,"amount":9090},{"price":22580,"amount":9190},{"price":22880,"amount":9295},{"price":23180,"amount":9395},{"price":23485,"amount":9495},{"price":23785,"amount":9595},{"price":24090,"amount":9695},{"price":24390,"amount":9800},{"price":24695,"amount":9900},{"price":25000,"amount":10000}],"idolPitcherWinTiers":[{"price":50,"amount":200},{"price":175,"amount":275},{"price":305,"amount":350},{"price":430,"amount":425},{"price":560,"amount":500},{"price":685,"amount":570},{"price":810,"amount":645},{"price":940,"amount":720},{"price":1065,"amount":795},{"price":1195,"amount":870},{"price":1320,"amount":945},{"price":1445,"amount":1020},{"price":1575,"amount":1095},{"price":1700,"amount":1170},{"price":1830,"amount":1245},{"price":1955,"amount":1315},{"price":2085,"amount":1390},{"price":2210,"amount":1465},{"price":2335,"amount":1540},{"price":2465,"amount":1615},{"price":2590,"amount":1690},{"price":2720,"amount":1765},{"price":2845,"amount":1840},{"price":2970,"amount":1915},{"price":3100,"amount":1990},{"price":3225,"amount":2060},{"price":3355,"amount":2135},{"price":3480,"amount":2210},{"price":3605,"amount":2285},{"price":3735,"amount":2360},{"price":3860,"amount":2435},{"price":3990,"amount":2510},{"price":4115,"amount":2585},{"price":4240,"amount":2660},{"price":4370,"amount":2735},{"price":4495,"amount":2805},{"price":4625,"amount":2880},{"price":4750,"amount":2955},{"price":4880,"amount":3030},{"price":5005,"amount":3105},{"price":5130,"amount":3180},{"price":5260,"amount":3255},{"price":5385,"amount":3330},{"price":5515,"amount":3405},{"price":5640,"amount":3480},{"price":5765,"amount":3550},{"price":5895,"amount":3625},{"price":6020,"amount":3700},{"price":6150,"amount":3775},{"price":6275,"amount":3850},{"price":6400,"amount":3925},{"price":6530,"amount":4000},{"price":6655,"amount":4075},{"price":6785,"amount":4150},{"price":6910,"amount":4220},{"price":7035,"amount":4295},{"price":7165,"amount":4370},{"price":7290,"amount":4445},{"price":7420,"amount":4520},{"price":7545,"amount":4595},{"price":7670,"amount":4670},{"price":7800,"amount":4745},{"price":7925,"amount":4820},{"price":8055,"amount":4895},{"price":8180,"amount":4965},{"price":8310,"amount":5040},{"price":8435,"amount":5115},{"price":8560,"amount":5190},{"price":8690,"amount":5265},{"price":8815,"amount":5340},{"price":8945,"amount":5415},{"price":9070,"amount":5490},{"price":9195,"amount":5565},{"price":9325,"amount":5640},{"price":9450,"amount":5710},{"price":9580,"amount":5785},{"price":9705,"amount":5860},{"price":9830,"amount":5935},{"price":9960,"amount":6010},{"price":10085,"amount":6085},{"price":10215,"amount":6160},{"price":10340,"amount":6235},{"price":10465,"amount":6310},{"price":10595,"amount":6385},{"price":10720,"amount":6455},{"price":10850,"amount":6530},{"price":10975,"amount":6605},{"price":11105,"amount":6680},{"price":11230,"amount":6755},{"price":11355,"amount":6830},{"price":11485,"amount":6905},{"price":11610,"amount":6980},{"price":11740,"amount":7055},{"price":11865,"amount":7130},{"price":11990,"amount":7200},{"price":12120,"amount":7275},{"price":12245,"amount":7350},{"price":12375,"amount":7425},{"price":12500,"amount":7500}],"idolPitcherLoseTiers":[{"price":50,"amount":200},{"price":165,"amount":280},{"price":285,"amount":360},{"price":400,"amount":440},{"price":515,"amount":520},{"price":635,"amount":600},{"price":750,"amount":680},{"price":870,"amount":755},{"price":985,"amount":835},{"price":1100,"amount":915},{"price":1220,"amount":995},{"price":1335,"amount":1075},{"price":1450,"amount":1155},{"price":1570,"amount":1235},{"price":1685,"amount":1315},{"price":1805,"amount":1395},{"price":1920,"amount":1475},{"price":2035,"amount":1555},{"price":2155,"amount":1635},{"price":2270,"amount":1710},{"price":2385,"amount":1790},{"price":2505,"amount":1870},{"price":2620,"amount":1950},{"price":2735,"amount":2030},{"price":2855,"amount":2110},{"price":2970,"amount":2190},{"price":3090,"amount":2270},{"price":3205,"amount":2350},{"price":3320,"amount":2430},{"price":3440,"amount":2510},{"price":3555,"amount":2590},{"price":3670,"amount":2665},{"price":3790,"amount":2745},{"price":3905,"amount":2825},{"price":4020,"amount":2905},{"price":4140,"amount":2985},{"price":4255,"amount":3065},{"price":4375,"amount":3145},{"price":4490,"amount":3225},{"price":4605,"amount":3305},{"price":4725,"amount":3385},{"price":4840,"amount":3465},{"price":4955,"amount":3545},{"price":5075,"amount":3620},{"price":5190,"amount":3700},{"price":5310,"amount":3780},{"price":5425,"amount":3860},{"price":5540,"amount":3940},{"price":5660,"amount":4020},{"price":5775,"amount":4100},{"price":5890,"amount":4180},{"price":6010,"amount":4260},{"price":6125,"amount":4340},{"price":6240,"amount":4420},{"price":6360,"amount":4500},{"price":6475,"amount":4580},{"price":6595,"amount":4655},{"price":6710,"amount":4735},{"price":6825,"amount":4815},{"price":6945,"amount":4895},{"price":7060,"amount":4975},{"price":7175,"amount":5055},{"price":7295,"amount":5135},{"price":7410,"amount":5215},{"price":7530,"amount":5295},{"price":7645,"amount":5375},{"price":7760,"amount":5455},{"price":7880,"amount":5535},{"price":7995,"amount":5610},{"price":8110,"amount":5690},{"price":8230,"amount":5770},{"price":8345,"amount":5850},{"price":8460,"amount":5930},{"price":8580,"amount":6010},{"price":8695,"amount":6090},{"price":8815,"amount":6170},{"price":8930,"amount":6250},{"price":9045,"amount":6330},{"price":9165,"amount":6410},{"price":9280,"amount":6490},{"price":9395,"amount":6565},{"price":9515,"amount":6645},{"price":9630,"amount":6725},{"price":9745,"amount":6805},{"price":9865,"amount":6885},{"price":9980,"amount":6965},{"price":10100,"amount":7045},{"price":10215,"amount":7125},{"price":10330,"amount":7205},{"price":10450,"amount":7285},{"price":10565,"amount":7365},{"price":10680,"amount":7445},{"price":10800,"amount":7520},{"price":10915,"amount":7600},{"price":11035,"amount":7680},{"price":11150,"amount":7760},{"price":11265,"amount":7840},{"price":11385,"amount":7920},{"price":11500,"amount":8000}],"teamShamedTiers":[{"price":100,"amount":200},{"price":105,"amount":325},{"price":110,"amount":450},{"price":120,"amount":575},{"price":130,"amount":700},{"price":140,"amount":830},{"price":155,"amount":955},{"price":170,"amount":1080},{"price":190,"amount":1205},{"price":205,"amount":1330},{"price":225,"amount":1455},{"price":250,"amount":1580},{"price":270,"amount":1705},{"price":295,"amount":1830},{"price":320,"amount":1955},{"price":345,"amount":2085},{"price":370,"amount":2210},{"price":395,"amount":2335},{"price":425,"amount":2460},{"price":455,"amount":2585},{"price":485,"amount":2710},{"price":515,"amount":2835},{"price":550,"amount":2960},{"price":580,"amount":3085},{"price":615,"amount":3210},{"price":650,"amount":3340},{"price":685,"amount":3465},{"price":725,"amount":3590},{"price":760,"amount":3715},{"price":800,"amount":3840},{"price":835,"amount":3965},{"price":875,"amount":4090},{"price":915,"amount":4215},{"price":960,"amount":4340},{"price":1000,"amount":4465},{"price":1045,"amount":4595},{"price":1085,"amount":4720},{"price":1130,"amount":4845},{"price":1175,"amount":4970},{"price":1220,"amount":5095},{"price":1270,"amount":5220},{"price":1315,"amount":5345},{"price":1365,"amount":5470},{"price":1410,"amount":5595},{"price":1460,"amount":5720},{"price":1510,"amount":5850},{"price":1560,"amount":5975},{"price":1610,"amount":6100},{"price":1665,"amount":6225},{"price":1715,"amount":6350},{"price":1770,"amount":6475},{"price":1825,"amount":6600},{"price":1880,"amount":6725},{"price":1935,"amount":6850},{"price":1990,"amount":6980},{"price":2045,"amount":7105},{"price":2100,"amount":7230},{"price":2160,"amount":7355},{"price":2215,"amount":7480},{"price":2275,"amount":7605},{"price":2335,"amount":7730},{"price":2395,"amount":7855},{"price":2455,"amount":7980},{"price":2515,"amount":8105},{"price":2580,"amount":8235},{"price":2640,"amount":8360},{"price":2705,"amount":8485},{"price":2765,"amount":8610},{"price":2830,"amount":8735},{"price":2895,"amount":8860},{"price":2960,"amount":8985},{"price":3025,"amount":9110},{"price":3090,"amount":9235},{"price":3160,"amount":9360},{"price":3225,"amount":9490},{"price":3295,"amount":9615},{"price":3360,"amount":9740},{"price":3430,"amount":9865},{"price":3500,"amount":9990},{"price":3570,"amount":10115},{"price":3640,"amount":10240},{"price":3715,"amount":10365},{"price":3785,"amount":10490},{"price":3855,"amount":10615},{"price":3930,"amount":10745},{"price":4000,"amount":10870},{"price":4075,"amount":10995},{"price":4150,"amount":11120},{"price":4225,"amount":11245},{"price":4300,"amount":11370},{"price":4375,"amount":11495},{"price":4450,"amount":11620},{"price":4530,"amount":11745},{"price":4605,"amount":11870},{"price":4685,"amount":12000},{"price":4760,"amount":12125},{"price":4840,"amount":12250},{"price":4920,"amount":12375},{"price":5000,"amount":12500}],"teamShamingTiers":[{"price":100,"amount":200},{"price":105,"amount":325},{"price":110,"amount":450},{"price":120,"amount":575},{"price":130,"amount":700},{"price":140,"amount":830},{"price":155,"amount":955},{"price":170,"amount":1080},{"price":190,"amount":1205},{"price":205,"amount":1330},{"price":225,"amount":1455},{"price":250,"amount":1580},{"price":270,"amount":1705},{"price":295,"amount":1830},{"price":320,"amount":1955},{"price":345,"amount":2085},{"price":370,"amount":2210},{"price":395,"amount":2335},{"price":425,"amount":2460},{"price":455,"amount":2585},{"price":485,"amount":2710},{"price":515,"amount":2835},{"price":550,"amount":2960},{"price":580,"amount":3085},{"price":615,"amount":3210},{"price":650,"amount":3340},{"price":685,"amount":3465},{"price":725,"amount":3590},{"price":760,"amount":3715},{"price":800,"amount":3840},{"price":835,"amount":3965},{"price":875,"amount":4090},{"price":915,"amount":4215},{"price":960,"amount":4340},{"price":1000,"amount":4465},{"price":1045,"amount":4595},{"price":1085,"amount":4720},{"price":1130,"amount":4845},{"price":1175,"amount":4970},{"price":1220,"amount":5095},{"price":1270,"amount":5220},{"price":1315,"amount":5345},{"price":1365,"amount":5470},{"price":1410,"amount":5595},{"price":1460,"amount":5720},{"price":1510,"amount":5850},{"price":1560,"amount":5975},{"price":1610,"amount":6100},{"price":1665,"amount":6225},{"price":1715,"amount":6350},{"price":1770,"amount":6475},{"price":1825,"amount":6600},{"price":1880,"amount":6725},{"price":1935,"amount":6850},{"price":1990,"amount":6980},{"price":2045,"amount":7105},{"price":2100,"amount":7230},{"price":2160,"amount":7355},{"price":2215,"amount":7480},{"price":2275,"amount":7605},{"price":2335,"amount":7730},{"price":2395,"amount":7855},{"price":2455,"amount":7980},{"price":2515,"amount":8105},{"price":2580,"amount":8235},{"price":2640,"amount":8360},{"price":2705,"amount":8485},{"price":2765,"amount":8610},{"price":2830,"amount":8735},{"price":2895,"amount":8860},{"price":2960,"amount":8985},{"price":3025,"amount":9110},{"price":3090,"amount":9235},{"price":3160,"amount":9360},{"price":3225,"amount":9490},{"price":3295,"amount":9615},{"price":3360,"amount":9740},{"price":3430,"amount":9865},{"price":3500,"amount":9990},{"price":3570,"amount":10115},{"price":3640,"amount":10240},{"price":3715,"amount":10365},{"price":3785,"amount":10490},{"price":3855,"amount":10615},{"price":3930,"amount":10745},{"price":4000,"amount":10870},{"price":4075,"amount":10995},{"price":4150,"amount":11120},{"price":4225,"amount":11245},{"price":4300,"amount":11370},{"price":4375,"amount":11495},{"price":4450,"amount":11620},{"price":4530,"amount":11745},{"price":4605,"amount":11870},{"price":4685,"amount":12000},{"price":4760,"amount":12125},{"price":4840,"amount":12250},{"price":4920,"amount":12375},{"price":5000,"amount":12500}],"incinerationTiers":[{"price":1000,"amount":100},{"price":1090,"amount":120},{"price":1185,"amount":140},{"price":1275,"amount":160},{"price":1365,"amount":180},{"price":1460,"amount":195},{"price":1550,"amount":215},{"price":1645,"amount":235},{"price":1735,"amount":255},{"price":1825,"amount":275},{"price":1920,"amount":295},{"price":2010,"amount":315},{"price":2100,"amount":335},{"price":2195,"amount":350},{"price":2285,"amount":370},{"price":2380,"amount":390},{"price":2470,"amount":410},{"price":2560,"amount":430},{"price":2655,"amount":450},{"price":2745,"amount":470},{"price":2835,"amount":490},{"price":2930,"amount":505},{"price":3020,"amount":525},{"price":3110,"amount":545},{"price":3205,"amount":565},{"price":3295,"amount":585},{"price":3390,"amount":605},{"price":3480,"amount":625},{"price":3570,"amount":645},{"price":3665,"amount":660},{"price":3755,"amount":680},{"price":3845,"amount":700},{"price":3940,"amount":720},{"price":4030,"amount":740},{"price":4120,"amount":760},{"price":4215,"amount":780},{"price":4305,"amount":800},{"price":4400,"amount":815},{"price":4490,"amount":835},{"price":4580,"amount":855},{"price":4675,"amount":875},{"price":4765,"amount":895},{"price":4855,"amount":915},{"price":4950,"amount":935},{"price":5040,"amount":955},{"price":5135,"amount":970},{"price":5225,"amount":990},{"price":5315,"amount":1010},{"price":5410,"amount":1030},{"price":5500,"amount":1050},{"price":5590,"amount":1070},{"price":5685,"amount":1090},{"price":5775,"amount":1110},{"price":5865,"amount":1130},{"price":5960,"amount":1145},{"price":6050,"amount":1165},{"price":6145,"amount":1185},{"price":6235,"amount":1205},{"price":6325,"amount":1225},{"price":6420,"amount":1245},{"price":6510,"amount":1265},{"price":6600,"amount":1285},{"price":6695,"amount":1300},{"price":6785,"amount":1320},{"price":6880,"amount":1340},{"price":6970,"amount":1360},{"price":7060,"amount":1380},{"price":7155,"amount":1400},{"price":7245,"amount":1420},{"price":7335,"amount":1440},{"price":7430,"amount":1455},{"price":7520,"amount":1475},{"price":7610,"amount":1495},{"price":7705,"amount":1515},{"price":7795,"amount":1535},{"price":7890,"amount":1555},{"price":7980,"amount":1575},{"price":8070,"amount":1595},{"price":8165,"amount":1610},{"price":8255,"amount":1630},{"price":8345,"amount":1650},{"price":8440,"amount":1670},{"price":8530,"amount":1690},{"price":8620,"amount":1710},{"price":8715,"amount":1730},{"price":8805,"amount":1750},{"price":8900,"amount":1765},{"price":8990,"amount":1785},{"price":9080,"amount":1805},{"price":9175,"amount":1825},{"price":9265,"amount":1845},{"price":9355,"amount":1865},{"price":9450,"amount":1885},{"price":9540,"amount":1905},{"price":9635,"amount":1920},{"price":9725,"amount":1940},{"price":9815,"amount":1960},{"price":9910,"amount":1980},{"price":10000,"amount":2000}],"consumerTiers":[{"price":10,"amount":50},{"price":50,"amount":80},{"price":105,"amount":110},{"price":170,"amount":140},{"price":245,"amount":170},{"price":325,"amount":200},{"price":405,"amount":230},{"price":495,"amount":260},{"price":585,"amount":290},{"price":685,"amount":320},{"price":780,"amount":350},{"price":885,"amount":380},{"price":990,"amount":410},{"price":1095,"amount":440},{"price":1205,"amount":470},{"price":1315,"amount":500},{"price":1430,"amount":530},{"price":1545,"amount":560},{"price":1665,"amount":590},{"price":1785,"amount":620},{"price":1910,"amount":650},{"price":2035,"amount":680},{"price":2160,"amount":710},{"price":2285,"amount":740},{"price":2415,"amount":770},{"price":2550,"amount":805},{"price":2680,"amount":835},{"price":2815,"amount":865},{"price":2950,"amount":895},{"price":3090,"amount":925},{"price":3225,"amount":955},{"price":3365,"amount":985},{"price":3510,"amount":1015},{"price":3650,"amount":1045},{"price":3795,"amount":1075},{"price":3940,"amount":1105},{"price":4090,"amount":1135},{"price":4235,"amount":1165},{"price":4385,"amount":1195},{"price":4535,"amount":1225},{"price":4685,"amount":1255},{"price":4840,"amount":1285},{"price":4990,"amount":1315},{"price":5145,"amount":1345},{"price":5305,"amount":1375},{"price":5460,"amount":1405},{"price":5620,"amount":1435},{"price":5775,"amount":1465},{"price":5935,"amount":1495},{"price":6100,"amount":1525},{"price":6260,"amount":1555},{"price":6425,"amount":1585},{"price":6585,"amount":1615},{"price":6750,"amount":1645},{"price":6915,"amount":1675},{"price":7085,"amount":1705},{"price":7250,"amount":1735},{"price":7420,"amount":1765},{"price":7590,"amount":1795},{"price":7760,"amount":1825},{"price":7930,"amount":1855},{"price":8105,"amount":1885},{"price":8275,"amount":1915},{"price":8450,"amount":1945},{"price":8625,"amount":1975},{"price":8800,"amount":2005},{"price":8975,"amount":2035},{"price":9155,"amount":2065},{"price":9330,"amount":2095},{"price":9510,"amount":2125},{"price":9690,"amount":2155},{"price":9870,"amount":2185},{"price":10050,"amount":2215},{"price":10230,"amount":2245},{"price":10415,"amount":2280},{"price":10595,"amount":2310},{"price":10780,"amount":2340},{"price":10965,"amount":2370},{"price":11150,"amount":2400},{"price":11335,"amount":2430},{"price":11525,"amount":2460},{"price":11710,"amount":2490},{"price":11900,"amount":2520},{"price":12090,"amount":2550},{"price":12280,"amount":2580},{"price":12470,"amount":2610},{"price":12660,"amount":2640},{"price":12850,"amount":2670},{"price":13045,"amount":2700},{"price":13235,"amount":2730},{"price":13430,"amount":2760},{"price":13625,"amount":2790},{"price":13820,"amount":2820},{"price":14015,"amount":2850},{"price":14210,"amount":2880},{"price":14405,"amount":2910},{"price":14605,"amount":2940},{"price":14800,"amount":2970},{"price":15000,"amount":3000}]}}
  }
}
```

## databaseSubleague
Gets subleague by ID for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseSubleague.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseSubleague.type = "live"
Returns data from Blaseball, time agnostic.

### databaseSubleague.type = "static"
Returns a static set of data, parsed from `databaseSubleague.data`.

Example:
```json
{
  "databaseSubleague": {
    "type": "static",
    "data": {
      "id": "4fe65afa-804f-4bb2-9b15-1281b2eab110",
      "divisions": [
        "456089f0-f338-4620-a014-9540868789c9",
        "fadc9684-45b3-47a6-b647-3be3f0735a84"
      ],
      "name": "The Mild League"
    }
  }
}
```

## databaseSunSun
Gets data for Sun(Sun) for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseSunSun.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseSunSun.type = "live"
Returns data from Blaseball, time agnostic.

### databaseSunSun.type = "static"
Returns a static set of data, parsed from `databaseSunSun.data`.

Example:
```json
{
  "databaseSunSun": {
    "type": "static",
    "data": {
      "current": 99999.79999999968,
      "maximum": 99999,
      "recharge": 26244
    }
  }
}
```

## databaseTeam
Get teams for the provided IDs for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseTeam.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseTeam.type = "live"
Returns data from Blaseball, time agnostic.

### databaseTeam.type = "static"
Returns a static set of data, parsed from `databaseTeam.data`.

Example:
```json
{
  "databaseTeam": {
    "type": "static",
    "data": {
      "id": "1a51664e-efec-45fa-b0ba-06d04c344628",
      "lineup": [
        "15b1b9d3-4921-4898-9f71-e33e8e11cae7",
        "6a9b768f-8154-476c-b293-95d15617e1d0",
        "8c00dbe3-47d6-4afb-8f29-92312a336548",
        "7dc5a50e-85c0-4e8b-b2d5-151b720342c9",
        "d7aa3455-072f-4133-bec5-55dd297d57ab",
        "bb1eb01a-04d4-4e57-97c8-34cb80b250d9",
        "5bf6faf9-392d-470f-95be-87276d0bc8e3",
        "e2425d71-ae9f-49d5-bf89-a733bf3fe1e0",
        "9b6c5cf3-af06-40ef-82de-ce4c88c452fe"
      ],
      "rotation": [
        "129a50ff-81a6-4860-9aed-116ae5db09ef",
        "37e74dc8-3d6c-49f5-a4d0-952a9260e7d7",
        "55886358-0dc0-4666-a483-eecb4e95bd7e",
        "93f50e7a-5e0e-40e3-8191-01c39007586b"
      ],
      "seasAttr": [],
      "permAttr": [
        "PSYCHIC"
      ],
      "fullName": "Oregon Psychics",
      "location": "Oregon",
      "mainColor": "#ad0155",
      "nickname": "Psychics",
      "secondaryColor": "#FA66AE",
      "shorthand": "PSY",
      "emoji": "0x1f9e0",
      "slogan": "We Already Know.",
      "shameRuns": 0,
      "totalShames": 21,
      "totalShamings": 21,
      "seasonShames": 0,
      "seasonShamings": 0,
      "championships": 0,
      "weekAttr": [],
      "gameAttr": [],
      "rotationSlot": 0,
      "teamSpirit": 0,
      "card": -1,
      "tournamentWins": 0,
      "stadium": null,
      "imPosition": [
        0,
        0
      ],
      "eDensity": 0,
      "state": {},
      "evolution": 0,
      "winStreak": 0,
      "level": 0,
      "shadows": [
        "1da06cee-e1fd-4939-a8b0-159d6cc67faa",
        "93784be0-1636-4ea6-82b3-6853a51a897f",
        "4cd63a32-7fae-4e79-bd6b-5d8740ec3529",
        "d69b57d8-9728-45d4-b888-07606c042d70",
        "076750ca-42de-496a-808e-b9ddfc3e8788",
        "c0210f64-e8a8-4a33-a40c-2b9210a2bfd4",
        "b39f63a9-827c-4f23-8873-7bf8f47de4e0",
        "4e328c64-85f0-4b6e-9ca3-146c1a886497",
        "be15d63d-9c93-4e26-867a-680a59d437a3",
        "ba13bd83-dc50-4f65-94f4-e3595244fc4c",
        "549772ce-3a43-4936-8e50-ee162f7ebf7b"
      ],
      "underchampionships": -1,
      "deceased": true
    }
  }
}
```

## databaseTeamElectionStats
Gets election stats for the provided team ID in this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseTeamElectionStats.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseTeamElectionStats.type = "live"
Returns data from Blaseball, time agnostic.

### databaseTeamElectionStats.type = "static"
Returns a static set of data, parsed from `databaseTeamElectionStats.data`.

Example:
```json
{
  "databaseTeamElectionStats": {
    "type": "static",
    "data": {
      "wills": [
        {
          "id": "item_steal",
          "percent": "50.0"
        },
        {
          "id": "mod_reroll",
          "percent": "25.0"
        },
        {
          "id": "shadow_revoke",
          "percent": "25.0"
        }
      ]
    }
  }
}
```

## databaseVault
Gets the players in the vault for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### databaseVault.type = "chronicler"
Returns data from Chronicler at the instance's time.

### databaseVault.type = "live"
Returns data from Blaseball, time agnostic.

### databaseVault.type = "static"
Returns a static set of data, parsed from `databaseVault.data`.

Example:
```json
{
  "databaseVault": {
    "type": "static",
    "data": {
      "legendaryPlayers": [
        "26545c1f-bdb9-41f5-bced-45238b45e9fc",
        "2efb1f0a-72de-4178-9e53-35cd3e9f7798",
        "195dc422-98b6-4c34-a0b9-7b7038331216",
        "560adb76-796d-40c8-9869-937994cd7eed",
        "86d4e22b-f107-4bcf-9625-32d387fcb521",
        "c0732e36-3731-4f1a-abdc-daa9563b6506",
        "a1628d97-16ca-4a75-b8df-569bae02bef9",
        "efafe75e-2f00-4418-914c-9b6675d39264",
        "864b3be8-e836-426e-ae56-20345b41d03d",
        "de21c97e-f575-43b7-8be7-ecc5d8c4eaff",
        "c675fcdf-6117-49a6-ac32-99a89a3a88aa",
        "fedbceb8-e2aa-4868-ac35-74cd0445893f",
        "d1a198d6-b05a-47cf-ab8e-39a6fa1ed831",
        "89ec77d8-c186-4027-bd45-f407b4800c2c",
        "e16c3f28-eecd-4571-be1a-606bbac36b2b",
        "5bcfb3ff-5786-4c6c-964c-5c325fcc48d7",
        "a253facf-a54a-493e-b398-cf6f0d288990",
        "ef9f8b95-9e73-49cd-be54-60f84858a285",
        "11de4da3-8208-43ff-a1ff-0b3480a0fbf1",
        "b082ca6e-eb11-4eab-8d6a-30f8be522ec4",
        "04e14d7b-5021-4250-a3cd-932ba8e0a889",
        "88cd6efa-dbf2-4309-aabe-ec1d6f21f98a",
        "f70dd57b-55c4-4a62-a5ea-7cc4bf9d8ac1",
        "04931546-1b4a-469f-b391-7ed67afe824c"
      ]
    }
  }
}
```

## eventsStreamData
Gets the event data as a SSE stream for this instance. Must either be a string, or an object.

Defaults to returning data from [Chronicler](https://github.com/xSke/Chronicler).

When it's a string, must be "chronicler", or "live".
When it's an object, it requires a property to define an endpoint.

### eventsStreamData.type = "chronicler"
Returns data from Chronicler at the instance's time.

### eventsStreamData.type = "live"
Returns data from Blaseball, time agnostic.

### eventsStreamData.type = "chronicler_at_time"
Returns data from Chronicler at a different time.

Parameters:
- eventsStreamData.clock is parsed as a [clock](#clock)

Example:
```json
{
  "eventsStreamData": {
    "type": "chronicler_at_time",
    "clock": "utc"
  }
}
```

### eventsStreamData.type = "static"
Returns a static set of data, parsed from `eventsStreamData.data`.

Example:
```json
{
  "eventsStreamData": {
    "type": "static",
    "data": [{"id":"456089f0-f338-4620-a014-9540868789c9","name":"Mild High","teams":["36569151-a2fb-43c1-9df7-2df512424c82","b024e975-1c4a-4575-8936-a3754a08806a","b72f3061-f573-40d7-832a-5ad475bd7909","23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","105bc3ff-1320-4e37-8ef0-8d595cb95dd0","46358869-dce9-4a01-bfba-ac24fc56f57e"]},{"id":"5eb2271a-3e49-48dc-b002-9cb615288836","name":"Chaotic Good","teams":["bfd38797-8404-4b38-8b82-341da28b1f83","3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","979aee4a-6d80-4863-bf1c-ee1a78e06024","7966eb04-efcc-499b-8f03-d13916330531","36569151-a2fb-43c1-9df7-2df512424c82"]},{"id":"765a1e03-4101-4e8e-b611-389e71d13619","name":"Lawful Evil","teams":["8d87c468-699a-47a8-b40d-cfb73a5660ad","23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7","f02aeae2-5e6a-4098-9842-02d2273f25c7","57ec08cc-0411-4643-b304-0e80dbc15ac7","747b8e4a-7e50-4638-a973-ea7950a3e739"]},{"id":"7fbad33c-59ab-4e80-ba63-347177edaa2e","name":"Chaotic Evil","teams":["eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","b63be8c2-576a-4d6e-8daf-814f8bcea96f","105bc3ff-1320-4e37-8ef0-8d595cb95dd0","a37f9158-7f82-46bc-908c-c9e2dda7c33b"]},{"id":"98c92da4-0ea7-43be-bd75-c6150e184326","name":"Wild Low","teams":["878c1bf6-0d21-4659-bfee-916c8314d69c","b63be8c2-576a-4d6e-8daf-814f8bcea96f","3f8bbb15-61c0-4e3f-8e4a-907a5fb1565e","f02aeae2-5e6a-4098-9842-02d2273f25c7","9debc64f-74b7-4ae1-a4d6-fce0144b6ea5","bb4a9de5-c924-4923-a0cb-9d1445f1ee5d"]},{"id":"d4cc18de-a136-4271-84f1-32516be91a80","name":"Wild High","teams":["c73b705c-40ad-4633-a6ed-d357ee2e2bcf","a37f9158-7f82-46bc-908c-c9e2dda7c33b","ca3f1c8c-c025-4d8e-8eef-5be6accbeb16","747b8e4a-7e50-4638-a973-ea7950a3e739","57ec08cc-0411-4643-b304-0e80dbc15ac7","d9f89a8a-c563-493e-9d64-78e4f9a55d4a"]},{"id":"f711d960-dc28-4ae2-9249-e1f320fec7d7","name":"Lawful Good","teams":["b72f3061-f573-40d7-832a-5ad475bd7909","878c1bf6-0d21-4659-bfee-916c8314d69c","b024e975-1c4a-4575-8936-a3754a08806a","adc5b394-8f76-416d-9ce9-813706877b84","ca3f1c8c-c025-4d8e-8eef-5be6accbeb16"]},{"id":"fadc9684-45b3-47a6-b647-3be3f0735a84","name":"Mild Low","teams":["979aee4a-6d80-4863-bf1c-ee1a78e06024","eb67ae5e-c4bf-46ca-bbbc-425cd34182ff","bfd38797-8404-4b38-8b82-341da28b1f83","7966eb04-efcc-499b-8f03-d13916330531","adc5b394-8f76-416d-9ce9-813706877b84","8d87c468-699a-47a8-b40d-cfb73a5660ad"]}]
  }
}
```