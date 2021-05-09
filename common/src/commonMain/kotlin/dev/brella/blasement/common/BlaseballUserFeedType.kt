package dev.brella.blasement.common

object BlaseballUserFeedType {
    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 0,
    "created": "2021-05-06T17:39:40.582Z",
    "category": 0,
    "metadata": {
    "snackId": "Team_Win",
    "coinsAfter": 182,
    "snackAfter": 1,
    "coinsBefore": 192,
    "snackBefore": 0
    }
    }
     */
    const val BOUGHT_SNACK = 0

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 1,
    "created": "2021-05-06T17:39:45.823Z",
    "category": 0,
    "metadata": {
    "snackId": "Team_Win",
    "coinsAfter": 185,
    "snackAfter": 0,
    "coinsBefore": 182,
    "snackBefore": 1
    }
    }
     */
    const val SOLD_SNACK = 1

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 2,
    "created": "2021-04-23T16:01:50.156Z",
    "category": 1,
    "metadata": {
    "targets": [
    "878c1bf6-0d21-4659-bfee-916c8314d69c",
    "5d8fb7ff-fc32-4ae1-b7e2-fa4aba43db2d"
    ],
    "coinsAfter": 2930,
    "coinsBefore": 4586
    }
    }
     */
    const val BET_ON_TEAM = 2

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 3,
    "created": "2021-04-23T17:34:48.277Z",
    "category": 2,
    "metadata": {
    "toast": "You bet 1656 on the {tnn}9debc64f-74b7-4ae1-a4d6-fce0144b6ea5 and won 2968 coins.",
    "betType": 0,
    "coinsAfter": 19566,
    "coinsBefore": 16598
    }
    }

    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 3,
    "created": "2021-04-23T17:24:46.783Z",
    "category": 2,
    "metadata": {
    "toast": "You bet 1274 on the {tnn}36569151-a2fb-43c1-9df7-2df512424c82 and lost",
    "betType": 0,
    "coinsAfter": 10518,
    "coinsBefore": 10518
    }
    }
     */
    const val BET_RESULT = 3

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 4,
    "created": "2021-04-25T18:15:05.878Z",
    "category": 2,
    "metadata": {
    "toast": "Eat the Rich! You were in the 99%. You were given 3749 coins.",
    "valueAfter": 4295,
    "valueBefore": 546
    }
    },
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 4,
    "created": "2021-04-24T17:02:34.520Z",
    "category": 2,
    "metadata": {
    "toast": "You earned 450 coins from 1 Baserunners cleared in Flood weather.",
    "valueAfter": 546,
    "valueBefore": 96
    }
    }
     */
    const val GAINED_COINS = 4

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 6,
    "created": "2021-04-26T03:04:51.879Z",
    "category": 1,
    "metadata": {
    "idolAfter": "Jaylen Hotdogfingers",
    "coinsAfter": 4095,
    "idolBefore": "Zoey Kirchner",
    "coinsBefore": 4295
    }
    }
     */
    const val NEW_IDOL = 6

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 7,
    "created": "2021-04-21T01:57:12.417Z",
    "category": 3,
    "metadata": {
    "entity": "Fax Machine",
    "stadiumId": "031670ae-97a5-4215-b1a1-98e9f1de7c50",
    "coinsAfter": 0,
    "coinsBefore": 27466
    }
    },
     */
    const val CONTRIBUTED_TO_RENOVATION = 7

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 8,
    "created": "2021-04-28T01:08:20.579Z",
    "category": 1,
    "metadata": {
    "snackId": "Peanuts",
    "snackAfter": null
    }
    }
     */
    const val ATE_PEANUTS = 8

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 9,
    "created": "2021-04-17T08:21:07.415Z",
    "category": 3,
    "metadata": {
    "snackId": "Peanuts",
    "snackAfter": 0,
    "snackBefore": 4000,
    "tributeName": "Chorby Soul"
    }
    },
     */
    const val OFFERED_TRIBUTE = 9

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 10,
    "created": "2021-04-24T01:25:01.676Z",
    "category": 3,
    "metadata": {
    "data": {
    "team1": "46358869-dce9-4a01-bfba-ac24fc56f57e"
    },
    "entity": "Library",
    "snackId": "Votes",
    "voteType": "0",
    "snackAfter": 0,
    "snackBefore": 258
    }
    }
     */
    const val CAST_VOTES = 10

    /**
    {
    "userId": "0fd603f8-54e5-4580-bc60-333668d9da00",
    "type": 12,
    "created": "2021-04-28T02:48:10.846Z",
    "category": 3,
    "metadata": {
    "eventId": "7197c2d1-2578-4e82-992b-65f2ff87ccaf",
    "snackId": "Peanuts",
    "snackAfter": 992,
    "snackBefore": 993
    }
    }
     */
    const val UPSHELLED_EVENT = 12
}