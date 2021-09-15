package dev.brella.blasement.data

sealed interface SiteTransformer {
    fun interface InitialBinaryTransformer : SiteTransformer {
        fun transform(data: ByteArray): ByteArray?
    }

    fun interface InitialTextTransformer : SiteTransformer {
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
            companion object {
                val BASE_REGEX = "\"/([^\"])".toRegex()
                val ROOT_REGEX = "\"/\"([>}])".toRegex()
                val PUSH_REGEX = "\\.(push)\\(\"/\"\\)".toRegex()
            }

            override fun transform(data: String): String =
                data.replace(BASE_REGEX, "\"$basePath/\$1")
                    .replace(ROOT_REGEX, "\"$basePath/\"\$1")
                    .replace(PUSH_REGEX, ".\$1(\"$basePath/\")")
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

        object AddNewBeingsJs : InitialTextTransformer {
            //switch(e){case 0:return l.a.createElement(_.Kc,null);case 1:return l.a.createElement(Fc,{style:{filter:i.lightMode?"invert(1)":"none"}});case 2:return t?l.a.createElement("div",{className:"BigDeal-Equity"}):zg(a.sim,"SIM_NO_COIN")?l.a.createElement("div",{className:"BigDeal-Equity BigDeal-Equity-Bye"}):zg(a.sim,"SIM_COIN_SCATTERED")?l.a.createElement("div",{className:"BigDeal-Equity BigDeal-Equity-Scattered"}):l.a.createElement("div",{className:"BigDeal-Equity"});case 4:return l.a.createElement(Lc,null);case 5:return l.a.createElement(zc,null);case 6:return t?l.a.createElement(l.a.Fragment,null):l.a.createElement(Xc.a,{id:"0et7jJ1zV_w"})}return null}
            val MESSAGE_ICON_REGEX = "switch\\s*\\((\\w+)\\)\\{(case \\d+:\\s*return (\\w+\\.\\w+\\.createElement).+?)\\}return null\\}".toRegex()
            override fun transform(data: String): String =
                data.replace(MESSAGE_ICON_REGEX) { match ->
                    "switch(${match.groupValues[1]}){${match.groupValues[2]}}return ${match.groupValues[3]}(\"div\",{className:\"BigDeal-Icon-\" + ${match.groupValues[1]}});}"
                }
        }
        object AddTweetStylesCss : InitialTextTransformer {
            val PARKER_STYLE =
                """
                    .BigDeal-Message-Style-50 {
                      font-family: "Lora", "Courier New", monospace, serif;
                      font-weight: 700;
                    }
                    
                    .BigDeal-Message-Icon-50 {
                      background: #000000;
                    }
                    
                    .BigDeal-Icon-50 {
                      background: url(https://backblase.brella.dev/assets/beings/Parker.png);
                      background-repeat: no-repeat;
                      background-size: contain;
                      background-position: 50%;
                      margin: auto;
                      width: 100%;
                      height: 100%;
                      max-width: 80vw;
                    }
                """.trimIndent()

            val ANCHOR_STYLE =
                """
                    .BigDeal-Message-Style-51 {
                      font-family: "Lora", "Courier New", monospace, serif;
                      font-weight: 700;
                    }
                    
                    .BigDeal-Message-Icon-51 {
                      background: #000000;
                    }
                    
                    .BigDeal-Icon-51 {
                      background: url(https://backblase.brella.dev/assets/beings/ged_5nAK_400x400.jpg);
                      background-repeat: no-repeat;
                      background-size: contain;
                      background-position: 50%;
                      margin: auto;
                      width: 100%;
                      height: 100%;
                      max-width: 80vw;
                    }
                """.trimIndent()
            override fun transform(data: String): String? =
                data.plus(PARKER_STYLE)
                    .plus(ANCHOR_STYLE)
        }

        object AllowCustomEmojis: InitialTextTransformer {
            val EMOJI_TO_STRING = "function (\\w+)\\(\\w\\)\\{var \\w=Number\\(\\w\\);return isNaN\\(\\w\\)\\?\\w:String.fromCodePoint\\(\\w\\)\\}".toRegex()
            val CREATE_ELEMENT = "(\\w\\.\\w)\\.createElement".toRegex()

            override fun transform(data: String): String {
                val regexLocation = EMOJI_TO_STRING.find(data) ?: return data
                val createElement = CREATE_ELEMENT.find(data) ?: return data

                return buildString {
                    append(data)

                    val proxy = createElement.groupValues[1]
                    val functionName = regexLocation.groupValues[1]

                    replace(regexLocation.range.first, regexLocation.range.last + 1, """
                        function ${functionName}(e,i,proxy){if ((i!==null && i!==undefined) && proxy !== undefined){return proxy.createElement("img",{src:i,class:"BlasementEmojiIcon"})};var t=Number(e);return isNaN(t)?e:String.fromCodePoint(t)}
                    """.trimIndent())

                    val simpleFunctionInvocationsOfTeam = "$functionName\\((\\w)\\.(homeEmoji|awayEmoji|homeTeamEmoji|awayTeamEmoji|emoji)\\)".toRegex()
                    var invocation = simpleFunctionInvocationsOfTeam.find(this)

                    while (invocation != null) {
                        replace(invocation.range.first, invocation.range.last + 1, "$functionName(${invocation.groupValues[1]}.${invocation.groupValues[2]},${invocation.groupValues[1]}.${invocation.groupValues[2].replace("emoji", "icon").replace("Emoji", "Icon")},$proxy)")
                        invocation = simpleFunctionInvocationsOfTeam.find(this, invocation.range.first)
                    }

                    val variableFunctionInvocationsOfTeam = "(\\w)\\s*=\\s*(\\w)\\.(homeEmoji|awayEmoji|homeTeamEmoji|awayTeamEmoji|emoji).+?($functionName\\((\\w)\\))".toRegex()
                    invocation = variableFunctionInvocationsOfTeam.find(this)

                    while (invocation != null) {
                        val invocationMatch = invocation.groups[4]!!.range
                        replace(invocationMatch.first, invocationMatch.last + 1, "$functionName(${invocation.groupValues[5]},${invocation.groupValues[2]}.${invocation.groupValues[3].replace("emoji", "icon").replace("Emoji", "Icon")},$proxy)")
                        invocation = variableFunctionInvocationsOfTeam.find(this, invocation.range.first)
                    }

                    val builderInvocations = "emoji\\s*:\\s*(\\w)\\.(homeEmoji|awayEmoji|homeTeamEmoji|awayTeamEmoji|emoji),".toRegex()
                    invocation = builderInvocations.find(this)

                    while (invocation != null) {
                        insert(invocation.range.last + 1, "icon:${invocation.groupValues[1]}.${invocation.groupValues[2].replace("emoji", "icon").replace("Emoji", "Icon")},")
                        invocation = builderInvocations.find(this, invocation.range.last + 1)
                    }
                }
            }
        }
        object AddCustomEmojisCss: InitialTextTransformer {
            override fun transform(data: String): String? =
                data.plus("""
                    .BlasementEmojiIcon {
                        width:75%;
                        margin:auto;
                        display:block;
                    }
                """.trimIndent())
        }

        fun transform(data: String): String?
    }

    fun interface FinalTextTransformer : SiteTransformer {
        data class ReplaceDateWithRegexTimeWebsocket(val basePath: String) : FinalTextTransformer {
            override fun transform(data: String): String =
                data.replace("new Date()", "time()")
                    .replace("new Date([^(])".toRegex()) { match -> "time()${match.groupValues[1]}" }
                    .plus(";let loc=window.location,new_uri;const source=new WebSocket((loc.protocol === \"https:\"?\"wss://\":\"ws://\")+loc.host+\"$basePath/api/time\");source.addEventListener('message',function(event){window.blasementTime=event.data});function time(){return window.blasementTime?new Date(parseInt(window.blasementTime)):new Date();}")
        }

        data class ReplaceDateWithCursedTimeWebsocket(val basePath: String): FinalTextTransformer {
            override fun transform(data: String): String =
                data.plus(""";
                    let loc=window.location,new_uri;
                    const source=new WebSocket((loc.protocol === "https:"?"wss://":"ws://")+loc.host+"$basePath/api/time");
                    source.addEventListener('message',function(event){window.blasementTime=event.data});
                    
                    //credits to allie
                    const CurrentDate = Date;

                    // cursed glue
                    var bind = Function.bind;
                    var unbind = bind.bind(bind);
            
                    function instantiate(constructor, args) {
                        return new (unbind(constructor, null).apply(null, args));
                    }
            
                    // trickery
                    Date = function(Date) {
                        for (var n of Object.getOwnPropertyNames(Date)) {
                            if (n in TrickeryDate) continue;
            
                            let desc = Object.getOwnPropertyDescriptor(Date,n);
                            Object.defineProperty(TrickeryDate,n,desc);
                        }
            
                        return TrickeryDate;
            
                        function TrickeryDate() {
                            if (arguments.length > 0) {
                                return instantiate(CurrentDate,arguments);
                            }
            
                            var date = instantiate(CurrentDate,arguments);
                            date = instantiate(CurrentDate,[parseInt(window.blasementTime)]);
                            return date;
                        }
                    }(Date);
                """.trimIndent())
        }

        fun transform(data: String): String?
    }

    fun interface FinalBinaryTransformer : SiteTransformer {
        fun transform(data: ByteArray): ByteArray?
    }
}