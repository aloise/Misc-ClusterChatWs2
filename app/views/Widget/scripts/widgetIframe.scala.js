@(
  widgetId:String,
  widgetConfig:play.api.libs.json.JsObject
)@layoutIframe(widgetId, widgetConfig) {

    function IBSBCIframeManager(domPrefix) {
        //this.IBSWidgetIFrame = "." + domPrefix + "-iframe";
        this.IBSWidgetIFrameWrap = "." + domPrefix + "-iframe-wrap";
        this.IBSIframeOpenedStateClass = domPrefix + "-wrap-opened";
        this.openChatFormButton = "#" + domPrefix + "-open-chat-form-button";
    };


    IBSBCIframeManager.prototype.init = function() {
        var self = this;

        $jq.receiveMessage(
            function(e){
                if (e && e.data) {
                    try {
                        var data = JSON.parse(e.data);
                        if ((typeof data !== 'undefined') && (typeof data.message !== 'undefined')) {
                            var message = data.message.toLowerCase();

                            if (message == 'IBS-Widget-Start-Complete'.toLowerCase()) {
                                $jq(self.IBSWidgetIFrameWrap).css("visibility", "visible").removeClass(self.IBSIframeOpenedStateClass);
                            }

                            if (message == 'IBS-Widget-Chat-Form-Opened'.toLowerCase()) {
                                $jq(self.IBSWidgetIFrameWrap).css("visibility", "visible").addClass(self.IBSIframeOpenedStateClass);
                            }

                            if (message == 'IBS-Widget-Chat-Form-Closed'.toLowerCase()) {
                                $jq(self.IBSWidgetIFrameWrap).css("visibility", "visible").removeClass(self.IBSIframeOpenedStateClass);
                                var width = data.params.width || 130;
                                var height = data.params.height || 36;
                                $jq(self.IBSWidgetIFrameWrap).css('width', width);
                                $jq(self.IBSWidgetIFrameWrap).css('height', height);
                            }

                            if (message == 'IBS-Widget-Ajax-Request-Error-Occurred'.toLowerCase()) {
                                ibsToastr.error(widgetConfig.viewTemplates.ajaxRequestErrorOccurred || "An error occurred. Please try again later..");
                            }

                            if (message == 'IBS-Widget-Offline-Message-Successfully-Sent'.toLowerCase()) {
                                ibsToastr.success(widgetConfig.viewTemplates.offlineMessageSuccessfullySent || "Thanks for the message! We'll get back to you as soon as we can.");
                            }

                            if (message == 'IBS-Widget-Chat-Transcript-Successfully-Sent'.toLowerCase()) {
                                ibsToastr.success(widgetConfig.viewTemplates.chatTranscriptSuccessfullySent || "Chat transcript was successfully sent to your email.");
                            }

                            if (message == 'IBS-Widget-Chat-Widget-Deleted'.toLowerCase()) {
                                $jq(self.IBSWidgetIFrameWrap).remove();
                            }
                        }
                    } catch(e) {
                    }
                }

            },
            window.location
        );
    };

    $jq(function() {
        var domPrefix = "ibs-business-chat";
        $jq("body").append(widgetHtml);

        var cssUrl = widgetConfig.baseURL + "assets/stylesheets/ui/uiIframe.css";
        var cssLink = $jq("<link rel='stylesheet' type='text/css' href='"+cssUrl+"'>");
        $jq("head").append(cssLink);

        var ibsbcIframeManager = new IBSBCIframeManager(domPrefix);
        ibsbcIframeManager.init();

    })

}