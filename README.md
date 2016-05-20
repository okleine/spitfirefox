## SPITFIREFOX - A CoAP Client for Android

SPITFIREFOX is based on [nCoAP](https://github.com/okleine/nCoAP).
The following screenshots (from a virtual Nexus 5 android device with API 23) exemplarly show a request to `coap://vs0.inf.ethz.ch/obs` and the response.

<img align="left" src="https://media.itm.uni-luebeck.de/people/kleine/spitfirefox-screenshots/request_fragment2.png" width="250"/>
<img align="right" src="https://media.itm.uni-luebeck.de/people/kleine/spitfirefox-screenshots/response_fragment2.png" width="250"/>
The **Request Fragment** on the left is to define a request incl. the target URI, the method and all options. The additional method *Discover* is to request the `/.well-known/core` resource of the given server and thus enable auto-completion for URI paths. 

The **Response Fragment** on the right is to display the response of the server. To save space, only options that were present in an incoming response are displayed in the response fragment. One can switch between both fragments with a swipe.

See also: [coSense - A CoAP Server for mobile Sensors attached to Android devices](https://github.com/okleine/coSense/)
