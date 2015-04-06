### Introduction ###
**WaRR** is a tool that records and replays with high fidelity the interaction between users and modern web applications.

WaRR consists of two independent components: the **WaRR Recorder** and the **WaRR Replayer**.

### The WaRR Recorder ###
**The WaRR Recorder is embedded in a web browser**. This design decision brings five advantages. First, WaRR has access to a user’s every click and keystroke, thus providing high-fidelity recording. Second, WaRR’s recorder requires no modification to web applications, being easy to employ by developers. Third, the recorder has access to the actual HTML code that will be rendered, after code has been dynamically loaded. Fourth, it can easily be extended to record various sources of nondeterminism (e.g., timers). Fifth, since the recorder is based on the web browser engine WebKit, which is used by a plethora of browsers, it can record user interactions across a large number of platforms.

### The WaRR Replayer ###
**The WaRR Replayer uses an enhanced, developer-specific web browser** that enables realistic simulation of user interaction. It is reasonable to expect developers’ browsers to have additional features compared to users’ browsers. For example, while normal WebKit-based web browsers prevent setting certain properties of JavaScript events, this restriction can be lifted during testing and debugging. Hence, the WaRR Replayer can correctly trigger JavaScript events (e.g., onKeyPress) and ensure that the associated event handlers run correctly.