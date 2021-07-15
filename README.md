# Blasement

Blasement is a mockup backend for [Blaseball](https://www.blaseball.com) using historical data retrieved from SIBR services like Chronicler, eventually, and upnuts.

Designed to be a drop-in for the live site in projects, Blasement allows configuration of instances, setting the start time, end time, temporal speed, and more.

![Blaseball, But Fast](https://cdn.discordapp.com/attachments/738107864605786253/841326892752961596/1SdxzjnLDX.gif)

Compared to other historical services like [Before](https://before.sibr.dev/) ([Github](https://github.com/iliana/before)), Blasement offers two different benefits in design goals:

- Maintaining local state on the server, allowing for transformations to be applied to said data once and reused for all clients, featuring things like speeding up or slowing down time, transforming elections, etc.

- Implementing the full api 'spec' on the backend, not just what is needed for the frontend (More on this [Soon](TM)).

Deployment is currently pending, so watch this space for more information and proper documentation!