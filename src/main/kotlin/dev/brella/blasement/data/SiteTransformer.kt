package dev.brella.blasement.data

sealed interface SiteTransformer {
    fun interface InitialBinaryTransformer: SiteTransformer {
        fun transform(data: ByteArray): ByteArray?
    }

    fun interface InitialTextTransformer: SiteTransformer {
        data class ReplaceStaticAssets(val basePath: String) : InitialTextTransformer {
            companion object {
                val MAIN_JS_REGEX = "\"(https://d35iw2jmbg6ut8.cloudfront.net)?/static/js/main\\..+\\.chunk\\.js\"".toRegex()
                val TWO_JS_REGEX = "\"(https://d35iw2jmbg6ut8.cloudfront.net)?/static/js/2\\..+\\.chunk\\.js\"".toRegex()
                val MAIN_CSS_REGEX = "\"(https://d35iw2jmbg6ut8.cloudfront.net)?/static/css/main\\..+\\.chunk\\.css\"".toRegex()
            }

            override fun transform(data: String): String =
                data.replace(MAIN_JS_REGEX, "\"$basePath/main.js\"")
                    .replace(TWO_JS_REGEX, "\"$basePath/2.js\"")
                    .replace(MAIN_CSS_REGEX, "\"$basePath/main.css\"")
        }

        data class ReplaceApiCalls(val basePath: String) : InitialTextTransformer {
            override fun transform(data: String): String =
                data.replace("\"/", "\"$basePath/")
        }

        object ReplaceFacebookWithDiscord : InitialTextTransformer {
            val CONTINUE_WITH_FACEBOOK_REGEX =
                "(\\w+\\.\\w+).createElement\\(\"a\",\\{className:\"Auth-SocialAuth\",href:\"auth/facebook\\?redirectUrl=\"\\.concat\\((\\w+)\\)},(?:\\w+.\\w+).createElement\\(\"div\",\\{className:\"Auth-SocialAuth-Icon-Container\"},(?:\\w+.\\w+).createElement\\((\\w+).(\\w),null\\)\\),\" Continue with Facebook\"\\),".toRegex()
            val CONTINUE_WITH_DISCORD_REPLACEMENT = { result: MatchResult ->
                "${result.value}${result.groupValues[1]}.createElement(\"a\",{className:\"Auth-SocialAuth\",href:\"auth/discord?redirectUrl=\".concat(${result.groupValues[2]})},${result.groupValues[1]}.createElement(\"div\",{className:\"Auth-SocialAuth-Icon-Container\"},${result.groupValues[1]}.createElement(${result.groupValues[3]}.${result.groupValues[4][0].dec()},null)),\" Continue with Discord\"),"
            }

            override fun transform(data: String): String =
                data.replace(CONTINUE_WITH_FACEBOOK_REGEX, CONTINUE_WITH_DISCORD_REPLACEMENT)
                    .replace("auth/facebook", "auth/discord")
        }

        fun transform(data: String): String?
    }

    fun interface FinalTextTransformer: SiteTransformer {
        data class ReplaceTimeWithWebsocket(val basePath: String) : FinalTextTransformer {
            override fun transform(data: String): String =
                data.replace("new Date()", "time()")
                    .replace("new Date([^(])".toRegex()) { match -> "time()${match.groupValues[1]}" }
                    .plus(";let loc=window.location,new_uri;const source=new WebSocket((loc.protocol === \"https:\"?\"wss://\":\"ws://\")+loc.host+\"$basePath/api/time\");source.addEventListener('message',function(event){window.blasementTime=event.data});function time(){return window.blasementTime?new Date(parseInt(window.blasementTime)):new Date();}")
        }

        fun transform(data: String): String?
    }

    fun interface FinalBinaryTransformer: SiteTransformer {
        fun transform(data: ByteArray): ByteArray?
    }
}