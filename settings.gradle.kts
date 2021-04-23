rootProject.name = "blasement-bets"

include(":common", ":client", ":server")

rootProject.children.forEach { child -> child.name = "blasement-${child.name}" }