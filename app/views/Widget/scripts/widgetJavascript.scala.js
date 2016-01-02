@(
    widgetId:String,
    widgetConfig:play.api.libs.json.JsObject
)

function IBSBC(domPrefix, widgetId) {
    this.domPrefix = domPrefix;
    this.uniqId = "bz51yrhfsd89y0fw";
    this.widgetId = widgetId;
    this.chatWrap = "." + this.domPrefix + "-wrap";
    this.openChatFormButton = "#" + this.domPrefix + "-open-chat-form-button";
    this.startChatButton = "#" + this.domPrefix + "-start-chat-button";
    this.sendOfflineMessageButton = "#" + this.domPrefix + "-send-offline-message-button";
    this.chatFooter = "." + this.domPrefix + "-footer";
    this.chatHeaderTitle = "." + this.domPrefix + "-header-title";
    this.chatHeader= "." + this.domPrefix + "-header";
    this.chatHeaderButtons= "." + this.domPrefix + "-header-buttons";


    // tracking cookie is empty from the start
    this.trackingCookie = "";
    this.trackingCookieName = "bchat-tracking-uid";

    this.chatFormOptionIcon = "." + this.domPrefix + "-icon";
    this.minimizeChatFormIcon = "." + this.domPrefix + "-icon-minimize";
    //this.maximizeChatFormIcon = "." + this.domPrefix + "-icon-maximize";
    this.finishChatFormIcon = "." + this.domPrefix + "-icon-finish";
    this.finishChatFormButton = "." + this.domPrefix + "-form-finish-chat-button";
    this.backToDialogButton = "." + this.domPrefix + "-form-button-back-button";
    this.isMinimized = true;
    //this.isMaximized = true;

    this.loginForm = "." + this.domPrefix + "-start-form";
    this.conversationForm = "." + this.domPrefix + "-conversation-form";
    this.dialogIsFinishedForm = "." + this.domPrefix + "-dialog-is-finished";
    this.conversationLog =  "#" + this.domPrefix + "-messages-history";
    this.sendOfflineMessageForm = "." + this.domPrefix + "-send-offline-message-form";
    this.messageBlock =  this.domPrefix + "-message-wrap";
    this.messageText =  this.domPrefix + "-message-text";
    this.lastMessageBlock =  this.domPrefix + "-last-message-wrap";
    this.lastMessageLine =  this.domPrefix + "-last-message-line";
    this.lastMessageAuthorId = "";
    this.authorMessagesGroup = [];
    this.authorMessageGroupTime = null;

    this.messagesGroupInterval = 1000*120;

    this.sendMessageButton = "#" + this.domPrefix + "-send-message-button";
    this.userMessageTextarea = "#" + this.domPrefix + "-user-message-input";

    this.clientNameInput = "#" + this.domPrefix + "-login-name-input";
    this.clientEmailInput = "#" + this.domPrefix + "-login-email-input";
    this.clientCompanyInput = "#" + this.domPrefix + "-login-company-input";

    this.offlineClientNameInput = "#" + this.domPrefix + "-offline-login-name-input";
    this.offlineClientEmailInput = "#" + this.domPrefix + "-offline-login-email-input";
    this.offlineClientCompanyInput = "#" + this.domPrefix + "-offline-login-company-input";
    this.offlineDescriptionTextarea = "#" + this.domPrefix + "-offline-description-textarea";
    //this.offlineStatusLabel = "." + this.domPrefix + "-offline-status-label";

    this.clientName = "";
    this.clientEmail = "";
    this.clientCompany = "";

    this.rememberMe = false;
    this.rememberMeLifetime = 30; //cookie lifetime in days
    this.rememberMeInput = "#" + this.domPrefix + "-remember-me-checkbox";

    this.problemIsSolved = true;
    this.problemIsNotSolvedForm = "." + this.domPrefix + "-problem-is-not-solved-contact-data";
    this.problemIsSolvedRadio = "." + this.domPrefix + "-problem-is-solved-radio";
    this.problemIsNotSolvedFormData = "." + this.domPrefix + "-problem-is-not-solved-contact-data";
    this.problemIsNotSolvedEmail = "." + this.domPrefix + "-problem-is-not-solved-email";
    this.problemIsNotSolvedPhone = "." + this.domPrefix + "-problem-is-not-solved-phone";
    this.problemIsNotSolvedComment = "." + this.domPrefix + "-problem-is-not-solved-comment";
    this.saveAndExitButton = "." + this.domPrefix + "-form-button-save-exit-button";
    this.connectionErrorMessage = "." + this.domPrefix + "-connection-error-message";
    this.socketIsConnectingPromise = null;

    this.clientCookieName = "user-" + this.widgetId + "-" + this.uniqId;
    this.clientCookieNameLifetime = 1; //cookie lifetime in days if remember me is not checked

    this.sockIsConnected = false; //is connected to socks server
    this.sockIsConnecting = false;
    //this.userIsConnected = false;
    this.userIsConnecting = false;
    //this.isAssistantOnline = false; // at least one assistant is online
    this.companyAssistants = {};

    this.chatRoomId = "";
    this.chatRoomIdCookieName = "chat-room-id-" + this.widgetId + "-" + this.uniqId;

    this.sock = null;
    this.chatRoomIsClosedByAssisant = false;

    this.keepAliveTimeout = 55000;
    this.pingPromise = null;
    this.pongPromise = null;

    this.sockIsReconnecting = false;
    this.reconnectPromise = null;
    this.startReconnectTimeout = 1000;
    this.reconnectRetry = 1;
    this.reconnectRetryLimit = 10;
    this.reconnectRetryLimitTimeout = 60000;
    this.socketAutoReconnectEnabled = true;

    //this.fatalConnectionError = false;
    this.atLeastOneAssistantIsOnline = true;
    this.assistantsOnlineStatusIsLoading = false;
    this.offlineMessageIsSubmitting = false;

    this.emailValidationRexep = /^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

    this.emailTranscriptButton = "." + this.domPrefix + "-email-transcript-button";

    //this.widgetAutoOpenPromise = null;
    this.widgetHoverAutoOpenPromise = null;

    this.messageUsernameClass = {
        'client': 'client',
        'assistant': 'assistant',
        'system': 'system',
        'greetingMessage': 'greeting-message'
    };
}

IBSBC.prototype.init = function() {
    var self = this;

    var cssUrl = widgetConfig.baseURL + "assets/stylesheets/ui/ui.css";
    var cssLink = $jq("<link rel='stylesheet' type='text/css' href='"+cssUrl+"'>");
    $jq("head").append(cssLink);

    $jq.cookie.defaults = {'path' : '/'};

    $jq.cookie.json = false;
    self.trackingCookie = $jq.cookie( self.trackingCookieName);

    var wColor = widgetConfig.widgetColor;
    var colorContrast = this.getContrastYIQ(wColor);
    var headerColor = "#ffffff"; //TODO
    if (colorContrast) {
        headerColor = '#333333';
    }

    var headerBorderColor = "#a8a8a8"; //TODO
    if (colorContrast) {
        headerBorderColor = '#cccccc';
    }

    $jq(self.openChatFormButton).addClass(widgetConfig.widgetPosition).css({'background-color': wColor, 'color': headerColor});
    $jq(self.chatHeader).css({'background-color': wColor});
    $jq(self.chatHeaderTitle).css({'color': headerColor});
    $jq(self.chatHeaderButtons).css({'border-left': '1px solid '+headerBorderColor});

    $jq(self.saveAndExitButton).css({'background-color': wColor, 'border': '1px solid '+wColor});
    $jq(self.chatWrap).addClass(widgetConfig.widgetPosition).addClass(widgetConfig.widgetSize).css({'border': '1px solid '+wColor});

    var ts = new Date().getTime();

    $jq.ajax({
        type: "GET",
        dataType: "json",
        url: widgetConfig.baseURL + "tracker/widget",
        data: { widgetId : self.widgetId , trackingCookie :  ( self.trackingCookie || "" ), ts: ts },
        crossDomain: true
    }).
        done(function(data) {
            if( data.cookie ){
                self.trackingCookie = data.cookie[self.trackingCookieName];

                $jq.cookie.json = false;
                $jq.cookie( self.trackingCookieName, data.cookie[self.trackingCookieName], { expires: 365 }  );
            }
        });


    self.bindEvents();

    $jq(self.chatHeaderTitle + " span").html(widgetConfig.widgetHeader);
    $jq(self.openChatFormButton + " span").html(widgetConfig.widgetHeader);


    soundManager.setup({
        url: widgetConfig.baseURL + 'assets/files/swf/',
        flashVersion: 9,
        onready: function() {
            /*
            self.userChatMessageFromAssistant = soundManager.createSound({
                id: 'userChatMessageFromAssistant',
                url: widgetConfig.baseURL + 'assets/files/sounds/userChatMessageFromAssistant.mp3'
            });
            */
        }
    });

    self.postIframeMessage('IBS-Widget-Start-Complete', this.getOpenChatFormButtonSize());

    self.checkWidgetTriggers(widgetConfig);
};


IBSBC.prototype.getChatRoomCloseReason = function(reason){
    if (!_.isUndefined(widgetConfig.viewTemplates['chatRoomCloseReason' + reason])) {
        reason = widgetConfig.viewTemplates['chatRoomCloseReason' + reason];
    }

    return reason;

};

IBSBC.prototype.checkWidgetTriggers = function(widgetConfig){
    var self = this;

    if ((typeof widgetConfig.widgetTriggerId !== "undefined") && (widgetConfig.widgetTriggerId != "none")) {
        var widgetTimeout = (typeof widgetConfig.widgetTriggerTimeout === "undefined" ? 50 : (widgetConfig.widgetTriggerTimeout * 1000));
        if (widgetTimeout < 50) {
            widgetTimeout = 50; //limit timeout to 50ms
        }

        if (widgetConfig.widgetTriggerId == "widgetLoad") {
            setTimeout(function () {
                self.openChatFormButtonClick();
            }, widgetTimeout);
        }

        if (widgetConfig.widgetTriggerId == "widgetHover") {
            $jq(self.openChatFormButton).mouseenter(function() {
                self.widgetHoverAutoOpenPromise = setTimeout(function () {
                    if (self.isMinimized) {
                        self.openChatFormButtonClick();
                    }
                }, widgetTimeout);
            });

            $jq(self.openChatFormButton).mouseleave(function() {
                clearTimeout(self.widgetHoverAutoOpenPromise);
            });
        }
    }
};

IBSBC.prototype.getContrastYIQ = function(hexcolor){
    var r = parseInt(hexcolor.substr(1,2),16);
    var g = parseInt(hexcolor.substr(3,2),16);
    var b = parseInt(hexcolor.substr(5,2),16);
    var yiq = ((r*299)+(g*587)+(b*114))/1000;
    return (yiq >= 128) ? true : false;
};

IBSBC.prototype.postIframeMessage = function(message, params){
    params = params || {};

    $jq.postMessage(
        JSON.stringify({message: message, params: params}),
        widgetConfig.refererURL,
        parent
    );
};

IBSBC.prototype.pulsateOpenChatButton = function(stop) {
    stop = stop || false;

    $jq(this.openChatFormButton).pulse('destroy');
    $jq(this.openChatFormButton).css('opacity', '1');

    if (!stop && this.isMinimized) {
        var pulsateProperties = {
            opacity: '0.6'
        };

        $jq(this.openChatFormButton).pulse(pulsateProperties, {
            duration: 1250,
            pulses: 10,
            interval: 800
        });

        if (!_.isUndefined(this.userChatMessageFromAssistant)) {
            this.userChatMessageFromAssistant.play();
        }
    }



};

IBSBC.prototype.showConversationDialog = function(autoUserLogin, isConversationFormBlocked) {
    isConversationFormBlocked = isConversationFormBlocked || false;
    var self = this;

    if (autoUserLogin) {
        if (isConversationFormBlocked) {
            self.blockConversationFormOnDisconnect(true);
        }

        $jq(this.loginForm).hide();
        $jq(this.dialogIsFinishedForm).hide();
        $jq(this.sendOfflineMessageForm).hide();
        $jq(this.conversationForm).show();

        this.checkAtLeastOneAssistantHasAvatar();

        $jq(this.chatWrap).show();
    }
};


IBSBC.prototype.showChatIsFinishedForm = function() {
    $jq(this.userMessageTextarea).val('');
    $jq(this.dialogIsFinishedForm).show();
    $jq(this.conversationForm).hide();
    $jq(this.sendOfflineMessageForm).hide();
    $jq(this.loginForm).hide();

    $jq(this.chatWrap).show();
};

IBSBC.prototype.showLoginForm = function() {
    $jq(this.dialogIsFinishedForm).hide();
    $jq(this.conversationForm).hide();
    $jq(this.sendOfflineMessageForm).hide();
    $jq(this.loginForm).show();

    $jq(this.chatWrap).show();
};

IBSBC.prototype.showSendOfflineMessageForm = function() {
    $jq(this.dialogIsFinishedForm).hide();
    $jq(this.conversationForm).hide();
    $jq(this.loginForm).hide();
    $jq(this.sendOfflineMessageForm).show();
    $jq(this.sendOfflineMessageForm).scrollTo(0, 200);

    $jq(this.chatFooter).html("").attr("title", "");
    $jq(this.connectionErrorMessage).html("");

    $jq(this.chatWrap).show();
};

IBSBC.prototype.blockConversationFormOnDisconnect = function(isBlocked) {
    var self = this;
    if (isBlocked) {
        clearTimeout(self.socketIsConnectingPromise);
        self.socketIsConnectingPromise = setTimeout(function () {
            if (self.sockIsConnecting) {
                $jq(self.userMessageTextarea).prop('disabled', true);

                var chatFooterText = "Connecting...";
                if (!_.isUndefined(widgetConfig.viewTemplates.chatWidgetConnecting)) {
                    chatFooterText = widgetConfig.viewTemplates.chatWidgetConnecting;
                }
                $jq(self.chatFooter).html(chatFooterText).attr("title", "");
            }
        }, 500);
    } else {
        clearTimeout(self.socketIsConnectingPromise);
        $jq(self.userMessageTextarea).prop('disabled', false);
        $jq(self.chatFooter).html("").attr("title", "");
    }
};

IBSBC.prototype.closeSocket = function(autoUserLogin) {
    if ((typeof self.sock !== "undefined") && (self.sock != null) && (self.sockIsReconnecting)) {
        this.sock.close();
    }
};

IBSBC.prototype.connect = function(autoUserLogin) {
    autoUserLogin = autoUserLogin || false;
    var self = this;

    if ((typeof self.sock === "undefined") || (self.sock == null) || (self.sockIsReconnecting)) {
        self.closeSocket();

        self.sock = new SockJS( widgetConfig.baseURL + widgetConfig.socksGateway, {}, { debug: false} );
        self.sockIsConnecting = true;

        self.sock.onopen = function() {
            self.socketOnConnect();
        };

        self.sock.onmessage = function(sockMessage) {
            self.socketOnMessage(sockMessage, autoUserLogin);
        };

        self.sock.onclose = function() {
           self.socketOnClose();
        };

        self.sock.removeHandler = function(event) {
            delete this['on' + event];
            return this;
        };

    } else {
        self.showConversationDialog(autoUserLogin);
    }
};

IBSBC.prototype.startKeepAlivePing = function() {
    var self = this;
    clearTimeout(self.pingPromise);
    self.pingPromise = setTimeout(function () {
        self.sendKeepAliveRequest();
    }, self.keepAliveTimeout);
};

IBSBC.prototype.sendKeepAliveRequest = function() {
    this.sendSockMessage("Ping", { }, new Date().getTime());
    this.waitForKeepAlivePong();
};

IBSBC.prototype.waitForKeepAlivePong = function() {
    var self = this;
    clearTimeout(self.pongPromise);
    self.pongPromise = setTimeout(function () {
        //reconnect on failed;
        self.socketOnClose();
    }, self.keepAliveTimeout);
};

IBSBC.prototype.socketOnConnect = function() {
    this.sockIsConnecting = false;
    this.sockIsConnected = true;
    //this.userIsConnected = false;
    this.userIsConnecting = true;
    this.sockIsReconnecting = false;
    this.reconnectRetry = 1;

    //this.fatalConnectionError = false;
    this.socketAutoReconnectEnabled = true;

    $jq(this.chatFooter).html("").attr("title", "");
    $jq(this.connectionErrorMessage).html("");

    clearTimeout(this.reconnectPromise);

    this.sendConnectRequestMessage();
};

IBSBC.prototype.socketOnClose = function() {
    var self = this;

    self.sock.removeHandler("message");
    self.sock.removeHandler("open");
    self.sock.removeHandler("close");
    delete self.sock;

    self.sockIsConnected = false;
    //self.userIsConnected = false;
    self.userIsConnecting = false;

    clearTimeout(self.reconnectPromise);
    clearTimeout(self.pingPromise);
    clearTimeout(self.pongPromise);

    if (self.socketAutoReconnectEnabled) {
        self.sockIsConnecting = true;
        self.sockIsReconnecting = true;

        var interval = ((self.reconnectRetry < self.reconnectRetryLimit) ? self.startReconnectTimeout * self.reconnectRetry * 2 : self.reconnectRetryLimitTimeout );
        self.reconnectRetry++;

        self.blockConversationFormOnDisconnect(true);
        self.reconnectPromise = setTimeout(function () {
            self.connect();
        }, interval);
    }

};

IBSBC.prototype.getAssistantNameById = function(assistantId) {
    var assistantName = "System";
    var assistant = this.companyAssistants[assistantId];
    if (!_.isUndefined(assistant)) {
        assistantName = assistant.name;
    }

    return assistantName;
};

IBSBC.prototype.removeWidgetOnDelete = function() {
    this.socketAutoReconnectEnabled = false;
    this.chatRoomId = "";
    this.setChatRoomIdCookie();

    $jq(this.chatWrap).hide();
    this.postIframeMessage('IBS-Widget-Chat-Widget-Deleted');
};


IBSBC.prototype.socketOnMessage = function(sockMessage, autoUserLogin) {
    var content = JSON.parse(sockMessage.data);
    //console.log(content);

    if (typeof content !== 'undefined') {
        var event = content.event;
        var created = content.created;

        switch(event) {
            case "UserChatMessageFromAssistant":
                //get assistant data by id
                this.pulsateOpenChatButton();
                this.checkAtLeastOneAssistantHasAvatar();
                this.composeAssistantMessage(content.message, created, content.assistantId);
                break;

            case "UserConnected":
                //this.userIsConnected = true;
                this.userIsConnecting = false;

                this.showConversationDialog(autoUserLogin);
                this.chatRoomId = content.chatRoomId;
                this.setChatRoomIdCookie();

                this.blockConversationFormOnDisconnect(false);

                this.startKeepAlivePing();
                this.chatRoomIsClosedByAssisant = false;

                if (content.chatRoomData && content.chatRoomData.connectedAssistants && (content.chatRoomData.connectedAssistants.length > 0)) {
                    var currAssistantId = null;
                    for (var j = 0; j < content.chatRoomData.connectedAssistants.length; j++) {
                        currAssistantId = content.chatRoomData.connectedAssistants[j];
                        if (!_.isObject(this.companyAssistants[currAssistantId])) {
                            this.companyAssistants[currAssistantId] = {};
                        }
                        this.companyAssistants[currAssistantId].isConnected = true;

                        var messageText = this.companyAssistants[currAssistantId].name + " has joined the chat.";
                        if (!_.isUndefined(widgetConfig.viewTemplates.assistantHasJoinedChatRoom)) {
                            messageText = widgetConfig.viewTemplates.assistantHasJoinedChatRoom.replace('[AssistantName]', this.companyAssistants[currAssistantId].name);
                        }

                        this.composeSystemMessage(messageText, null);
                    }
                }

                this.checkAtLeastOneAssistantHasAvatar();


                if (content.chatRoomData && content.chatRoomData.messages && (content.chatRoomData.messages.length > 0)) {
                    $jq(this.conversationLog).find("." + this.domPrefix + "-message-author-" + this.messageUsernameClass["client"]).remove();
                    $jq(this.conversationLog).find("." + this.domPrefix + "-message-author-" + this.messageUsernameClass["assistant"]).remove();
                    $jq(this.conversationLog).find("." + this.domPrefix + "-message-author-" + this.messageUsernameClass["system"]).remove();

                    for (var k = 0; k < content.chatRoomData.messages.length; k++) {
                        var currMessage = content.chatRoomData.messages[k];
                        var currMessageCreated = currMessage.created;
                        var currMessageContent = currMessage.message;

                        switch (currMessage.fromType) {
                            case 0:
                                this.showMessage("client", currMessageContent, currMessageCreated, {isUser: true}, true);
                                break;
                            case 1:
                                this.composeAssistantMessage(currMessageContent, currMessageCreated, currMessage.from["$oid"], true);
                                break;
                            case 2:
                                if (typeof currMessage.messageType === "undefined") {
                                    this.composeSystemMessage(currMessageContent, currMessageCreated, true);
                                }
                                break;
                        }
                    }
                }

                break;

            case "UserConnectFailed":

                break;

            case "UserGreeting":
                this.lastMessageAuthorId = "";
                this.authorMessagesGroup = [];
                this.authorMessageGroupTime = null;

                this.companyAssistants = {};
                if (content.assistants && (content.assistants.length > 0)) {
                    var currAssistant = null;
                    for (var i = 0; i < content.assistants.length; i++) {
                        currAssistant =  content.assistants[i];
                        this.companyAssistants[currAssistant["_id"]["$oid"]] = {
                            id: currAssistant["_id"]["$oid"],
                            name: currAssistant.displayName,
                            avatar: ((currAssistant.avatar && currAssistant.avatar.length) ?  widgetConfig.resourceURL + currAssistant.avatar : ""),
                            isConnected: false
                        };
                    }
                }

                $jq(this.conversationLog).find("." + this.domPrefix + "-message-author-" + this.messageUsernameClass["greetingMessage"]).remove();
                this.showMessage("GreetingMessage", content.chatUserGreetingMessage, created, {isGreetingMessage: true});
                break;

            case "UserChatMessageFromSystem":
                this.pulsateOpenChatButton();
                this.composeSystemMessage(content.message, created);
                break;

            case "UserChatRoomClosed":
                this.socketAutoReconnectEnabled = false;
                this.chatRoomId = "";
                this.setChatRoomIdCookie();
                break;

            case "UserChatRoomClosedBySystem":
                this.socketAutoReconnectEnabled = false;
                this.chatRoomId = "";
                this.setChatRoomIdCookie();

                if (content.reason && content.reason.length && (content.reason == "WidgetDeleted")) {
                    this.removeWidgetOnDelete();
                }

                break;

            case "UserChatRoomClosedByAssistant":
                this.socketAutoReconnectEnabled = false;
                $jq(this.conversationLog).html("");
                $jq(this.userMessageTextarea).val('');

                this.chatRoomIsClosedByAssisant = true;
                var assistantName = this.getAssistantNameById(content.assistantId);

                if (!_.isUndefined(widgetConfig.viewTemplates.chatRoomClosedByAssistant)) {
                    $jq(this.chatFooter).html(widgetConfig.viewTemplates.chatRoomClosedByAssistant.replace('[AssistantName]', assistantName).replace('[ChatIsClosedByAssistantReason]',  "<b>" + this.getChatRoomCloseReason(content.reason) + "</b>"));
                    $jq(this.chatFooter).attr("title", widgetConfig.viewTemplates.chatRoomClosedByAssistant.replace('[AssistantName]', assistantName).replace('[ChatIsClosedByAssistantReason]',  this.getChatRoomCloseReason(content.reason)));
                } else {
                    $jq(this.chatFooter).html("Closed by " + assistantName + " as <b>" + this.getChatRoomCloseReason(content.reason) + "</b>");
                    $jq(this.chatFooter).attr("title", "Closed by " + assistantName + " as " + this.getChatRoomCloseReason(content.reason) + "");
                }

                this.dialogIsFinishedClick();

                break;

            case "UserAssistantChatRoomJoin":
                this.companyAssistants[content.assistantId] = {
                    id: content.assistantId,
                    name: content.name,
                    avatar: ((content.avatar && content.avatar.length) ?  widgetConfig.resourceURL + content.avatar : ""),
                    isConnected: true
                };
                this.checkAtLeastOneAssistantHasAvatar();
                created = new Date().getTime();

                var messageText = content.name + " has joined the chat.";
                if (!_.isUndefined(widgetConfig.viewTemplates.assistantHasJoinedChatRoom)) {
                    messageText = widgetConfig.viewTemplates.assistantHasJoinedChatRoom.replace('[AssistantName]', content.name);
                }

                this.composeSystemMessage(messageText, created);

                break;

            case "UserAssistantChatRoomLeave":
                var assistantName = this.getAssistantNameById(content.assistantId);
                this.companyAssistants[content.assistantId].isConnected = false;
                this.checkAtLeastOneAssistantHasAvatar();
                created = new Date().getTime();

                var messageText = assistantName + " has leaved the chat.";
                if (!_.isUndefined(widgetConfig.viewTemplates.assistantHasLeavedChatRoom)) {
                    messageText = widgetConfig.viewTemplates.assistantHasLeavedChatRoom.replace('[AssistantName]', assistantName);
                }

                this.composeSystemMessage(messageText, created);
                break;

            case "Error":
                //this.fatalConnectionError = true;
                this.socketAutoReconnectEnabled = false;
                this.chatRoomId = "";
                this.setChatRoomIdCookie();

                this.blockConversationFormOnDisconnect(false);

                $jq(this.connectionErrorMessage).html(content.description);
                $jq(this.conversationLog).html("");
                $jq(this.userMessageTextarea).val('');

                this.showLoginForm();

                if (content.description && content.description.length && ((content.description == "WidgetDeleted") || (content.description == "WidgetNotFound"))) {
                    this.removeWidgetOnDelete();
                }
                break;

            case "Pong":
                clearTimeout(this.pongPromise);
                this.startKeepAlivePing();
                break;

            case "AssistantInfoUpdated":
                var assistantId = content.assistant["_id"]["$oid"];
                if (_.isUndefined(this.companyAssistants[assistantId])) {
                    this.companyAssistants[assistantId] = {
                        isConnected: false
                    }
                }
                this.companyAssistants[assistantId] = {
                    name: content.assistant.displayName,
                    avatar: ((content.assistant.avatar && content.assistant.avatar.length) ?  widgetConfig.resourceURL + content.assistant.avatar : "")
                };
                if (!_.isUndefined(currAssistant) && (!currAssistant.avatar || currAssistant.avatar && (currAssistant.avatar.length == 0))) {
                    this.checkAtLeastOneAssistantHasAvatar();
                }
                break;

            default:
                //console.log('unknown event');
                break;

        }
    }
};

IBSBC.prototype.checkCurrentChatRoomId = function() {
    $jq.cookie.json = true;
    var chatRoomIdCookie = $jq.cookie(this.chatRoomIdCookieName);

    if ((typeof chatRoomIdCookie !== "undefined") && !($jq.isEmptyObject(chatRoomIdCookie))) {
        this.chatRoomId = chatRoomIdCookie.chatRoomId;
        if (this.chatRoomId.length > 0) {
            this.openChatFormButtonClick(true);
        }
    }
};

IBSBC.prototype.setChatRoomIdCookie = function() {
    $jq.cookie.json = true;
    $jq.cookie(this.chatRoomIdCookieName, {
        chatRoomId: this.chatRoomId
    }, { expires: 0.25 });

};

IBSBC.prototype.checkAtLeastOneAssistantHasAvatar = function() {
    var assistantsWithAvatar = _.filter(this.companyAssistants, function(assistant){ return assistant.avatar != ""; });
    var noAvatarsClass = this.domPrefix + '-no-avatars';
    if (_.isEmpty(assistantsWithAvatar)) {
        $jq(this.conversationLog).addClass(noAvatarsClass);
    } else {
        $jq(this.conversationLog).removeClass(noAvatarsClass);
    }
};

IBSBC.prototype.sendConnectRequestMessage = function() {
    this.sendSockMessage("UserConnectRequest", {
        user : {
            name: this.clientName,
            email: this.clientEmail,
            company: this.clientCompany,
            page: window.location.href,
            trackingCookie: this.trackingCookie
        },
        chatRoomId: this.chatRoomId,
        widgetId: this.widgetId
    }, new Date().getTime());
};

IBSBC.prototype.sendChatIsFinishedMessage = function() {
    this.sendSockMessage("UserChatFinished", {
        email: $jq(this.problemIsNotSolvedEmail).val(),
        phone: $jq(this.problemIsNotSolvedPhone).val(),
        comment: $jq(this.problemIsNotSolvedComment).val(),
        problemIsSolved: (this.problemIsSolved == "no" ? false : true)

    }, new Date().getTime());
    this.socketAutoReconnectEnabled = false;
};

IBSBC.prototype.sendChatIsFinishedAjaxMessage = function() {
    var url = widgetConfig.services.saveChatFeedback.replace("$chatRoomId", this.chatRoomId);
    var self = this;

    var data = {
        email: $jq(this.problemIsNotSolvedEmail).val(),
        phone: $jq(this.problemIsNotSolvedPhone).val(),
        comment: $jq(this.problemIsNotSolvedComment).val(),
        problemIsSolved: (this.problemIsSolved == "no" ? false : true)
    };

    $jq.ajax({
        url: url,
        type:'POST',
        data: JSON.stringify(data),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    });

    self.chatRoomIsClosedByAssisant = false;
    $jq(self.chatFooter).html("").attr("title", "");

    this.socketAutoReconnectEnabled = false;
};


IBSBC.prototype.getClientCookie = function() {
    var result = false;

    $jq.cookie.json = true;
    var clientCookie = $jq.cookie(this.clientCookieName);
    if ((typeof clientCookie !== "undefined") && !($jq.isEmptyObject(clientCookie))) {
        this.clientName = clientCookie.name;
        this.clientEmail = clientCookie.email;
        this.clientCompany = clientCookie.company;

        $jq(this.clientNameInput).val(this.clientName);
        $jq(this.clientEmailInput).val(this.clientEmail);
        $jq(this.clientCompanyInput).val(this.clientCompany);

        $jq(this.offlineClientNameInput).val(this.clientName);
        $jq(this.offlineClientEmailInput).val(this.clientEmail);
        $jq(this.offlineClientCompanyInput).val(this.clientCompany);

        result = true;
    }

    return result;
};


IBSBC.prototype.setClientCookie = function() {
    $jq.cookie.json = true;
    $jq.cookie(this.clientCookieName, {
        name: this.clientName,
        email: this.clientEmail,
        company: this.clientCompany
    }, { expires: ( this.rememberMe ? this.rememberMeLifetime : this.clientCookieNameLifetime) });
};


IBSBC.prototype.checkClientCookieAndShowConversationForm = function() {
    if (this.getClientCookie()) {
        this.showConversationDialog(true, true);
        this.checkAtLeastOneAssistantHasAvatar();
        this.connect(true);
        this.scrollToLastMessage(true);
    } else {
        this.showLoginForm();
    }
};

IBSBC.prototype.checkAtLeastOneAssistantIsOnline = function(chatRoomIdIsSet) {
    var self = this;
    if (chatRoomIdIsSet) {
        $jq(this.openChatFormButton).css('visibility', 'hidden');
        this.isMinimized = false;
        this.postIframeMessage('IBS-Widget-Chat-Form-Opened');

        self.checkClientCookieAndShowConversationForm();
    } else {
        if (!self.assistantsOnlineStatusIsLoading) {
            self.assistantsOnlineStatusIsLoading = true;
            $jq(self.openChatFormButton).addClass('ibs-business-chat-disabled');

            var ts = new Date().getTime();
            $jq.ajax({
                type: "GET",
                dataType: "json",
                url: widgetConfig.baseURL + "user/chat/check_assistants_online_status/" + self.widgetId,
                data: { ts: ts },
                crossDomain: true
            })
                .success(function (data) {
                    $jq(self.openChatFormButton).css('visibility', 'hidden');
                    self.isMinimized = false;

                    self.postIframeMessage('IBS-Widget-Chat-Form-Opened');
                    self.assistantsOnlineStatusIsLoading = false;
                    $jq(self.openChatFormButton).removeClass('ibs-business-chat-disabled');

                    if (!_.isUndefined(data.assistants) && (data.assistants.length > 0)) {
                        self.atLeastOneAssistantIsOnline = true;
                        self.checkClientCookieAndShowConversationForm();
                    } else {
                        self.atLeastOneAssistantIsOnline = false;
                        self.getClientCookie();
                        self.showSendOfflineMessageForm();
                    }
                })
                .error(function (data, status) {
                    var jsonData = null;
                    if (data && !_.isEmpty(data) && data.responseText) {
                        jsonData = $jq.parseJSON(data.responseText);
                    }

                    if (jsonData && jsonData.message && (jsonData.message == 'widget_not_found')) {
                       self.removeWidgetOnDelete();
                    } else {
                        self.atLeastOneAssistantIsOnline = false;
                        $jq(self.openChatFormButton).css('visibility', 'hidden');
                        self.isMinimized = false;

                        self.postIframeMessage('IBS-Widget-Chat-Form-Opened');
                        self.assistantsOnlineStatusIsLoading = false;
                        $jq(self.openChatFormButton).removeClass('ibs-business-chat-disabled');

                        self.getClientCookie();
                        self.showSendOfflineMessageForm();
                    }
                });
        }
    }
};

IBSBC.prototype.openChatFormButtonClick = function(chatRoomIdIsSet) {
    chatRoomIdIsSet = chatRoomIdIsSet || false;
    clearTimeout(this.widgetHoverAutoOpenPromise);

    if (!this.chatRoomIsClosedByAssisant) {
        this.checkAtLeastOneAssistantIsOnline(chatRoomIdIsSet);
    } else {
        $jq(this.openChatFormButton).css('visibility', 'hidden');
        this.isMinimized = false;
        this.postIframeMessage('IBS-Widget-Chat-Form-Opened');

        this.showChatIsFinishedForm();
    }

    $jq(this.connectionErrorMessage).html("");
    this.pulsateOpenChatButton(true);
};

IBSBC.prototype.getOpenChatFormButtonSize = function() {
    var width = $jq(this.openChatFormButton).css('width').replace('px', '');
    var pLeft = $jq(this.openChatFormButton).css('padding-left').replace('px', '');
    var pRight = $jq(this.openChatFormButton).css('padding-right').replace('px', '');
    width = (parseInt(width) + parseInt(pLeft) + parseInt(pRight)).toString() + 'px'

    var height = $jq(this.openChatFormButton).css('height');
    return { width: width, height: height};
};

IBSBC.prototype.showOpenChatFormButton = function() {
    var self = this;
    setTimeout(function() { $jq(self.openChatFormButton).css("visibility", "visible"); }, 100);
};

IBSBC.prototype.minimizeChatFormIconClick = function() {
    $jq(this.chatWrap).hide();
    this.isMinimized = true;

    this.postIframeMessage('IBS-Widget-Chat-Form-Closed', this.getOpenChatFormButtonSize());
    this.showOpenChatFormButton();

};

IBSBC.prototype.bindEvents = function() {
    var self = this;
    $jq(self.openChatFormButton).click(function() {
        self.openChatFormButtonClick();
    });

    $jq(self.sendOfflineMessageButton).click(function() {
        self.sendOfflineMessageButtonClick();
    });

    $jq(self.minimizeChatFormIcon).click(function() {
        self.minimizeChatFormIconClick();
    });

    $jq(self.backToDialogButton).click(function() {
        $jq(self.loginForm).hide();
        $jq(self.dialogIsFinishedForm).hide();
        $jq(self.conversationForm).show();

    });

    $jq(self.finishChatFormButton).click(function() {
        self.dialogIsFinishedClick();
    });

    $jq(self.finishChatFormIcon).click(function() {
        $jq.cookie.json = true;
        var clientCookie = $jq.cookie(self.clientCookieName);
        if (self.atLeastOneAssistantIsOnline && (typeof clientCookie !== "undefined") && !($jq.isEmptyObject(clientCookie))) {
            self.dialogIsFinishedClick();
        } else {
            self.minimizeChatFormIconClick();
        }
    });

    $jq(self.startChatButton).click(function() {
        self.startConversation();
    });

    $jq(self.sendMessageButton).click(function() {
        self.sendMessage();
        $jq(self.userMessageTextarea).focus();
    });

    $jq(self.userMessageTextarea).keydown(function(e) {
        if (e.keyCode == 13) {
            self.sendMessage();
            e.preventDefault();
        }
    });

    $jq(self.problemIsSolvedRadio).click(function() {
        self.problemIsSolved = $jq(this).val();
        self.problemIsSolvedClick();
    });

    $jq(self.saveAndExitButton).click(function() {
        self.saveAndExitButtonClick();
    });

    $jq(self.emailTranscriptButton).click(function() { //TODO
        self.emailTranscriptButtonClick();
    });


    $jq(self.clientNameInput).on("keyup change", function(e) { self.validateForm(e, false, self.loginForm)} );
    $jq(self.clientEmailInput).on("keyup change", function(e) { self.validateForm(e, false, self.loginForm)} );
    $jq(self.clientCompanyInput).on("keyup change", function(e) { self.validateForm(e, false, self.loginForm)} );

    $jq(self.clientNameInput).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.loginForm)} );
    $jq(self.clientEmailInput).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.loginForm)} );

    $jq(self.problemIsNotSolvedEmail).on("keyup change", function(e) { self.validateForm(e, false, self.problemIsNotSolvedForm)} );
    $jq(self.problemIsNotSolvedComment).on("keyup change", function(e) { self.validateForm(e, false, self.problemIsNotSolvedForm)} );
    $jq(self.problemIsNotSolvedPhone).on("keyup change", function(e) { self.validateForm(e, false, self.problemIsNotSolvedForm)} );

    $jq(self.problemIsNotSolvedEmail).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.problemIsNotSolvedForm)} );

    $jq(self.offlineClientNameInput).on("keyup change", function(e) { self.validateForm(e, false, self.sendOfflineMessageForm)} );
    $jq(self.offlineClientEmailInput).on("keyup change", function(e) { self.validateForm(e, false, self.sendOfflineMessageForm)} );
    $jq(self.offlineClientCompanyInput).on("keyup change", function(e) { self.validateForm(e, false, self.sendOfflineMessageForm)} );
    $jq(self.offlineDescriptionTextarea).on("keyup change", function(e) { self.validateForm(e, false, self.sendOfflineMessageForm)} );

    $jq(self.offlineClientNameInput).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.sendOfflineMessageForm)} );
    $jq(self.offlineClientEmailInput).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.sendOfflineMessageForm)} );
    $jq(self.offlineDescriptionTextarea).on("paste cut", function(e) { self.timeoutValidateForm(e, false, self.sendOfflineMessageForm)} );

    $jq(self.chatFormOptionIcon).hover(
        function() {
            $jq(this).addClass(self.domPrefix + "-icon-hover");
        }, function() {
            $jq(this).removeClass(self.domPrefix + "-icon-hover");
        }
    );
};

IBSBC.prototype.timeoutValidateForm = function(e, executeAfterValidate, form) {
    var self = this;
    setTimeout(function() {
        self.validateForm(e, executeAfterValidate, form);
    }, 0);

};

IBSBC.prototype.saveAndExitButtonClick = function() {
    var formIsValid = true;

    if (this.problemIsSolved == "no") {
        formIsValid = this.validateForm(null, false, this.problemIsNotSolvedForm);
    }

    if (formIsValid) {
        $jq(this.userMessageTextarea).val('');

        $jq(this.chatWrap).hide();

        $jq(this.loginForm).hide();
        $jq(this.dialogIsFinishedForm).hide();
        $jq(this.conversationForm).show();

        this.lastMessageAuthorId = "";
        this.authorMessagesGroup = [];
        this.authorMessageGroupTime = null;

        this.isMinimized = true;
        this.postIframeMessage('IBS-Widget-Chat-Form-Closed', this.getOpenChatFormButtonSize());

        this.showOpenChatFormButton();

        if (!this.chatRoomIsClosedByAssisant) {
            this.sendChatIsFinishedMessage();
        } else {
            this.sendChatIsFinishedAjaxMessage();
        }

        this.chatRoomId = "";
        this.setChatRoomIdCookie();

        $jq(this.conversationLog).html("");
    }
};


IBSBC.prototype.validateForm = function(e, executeAfterValidate, form) {
    var self = this;
    var dirtyInput = self.domPrefix + "-dirty-input";
    var errorInput = self.domPrefix + "-error-input";
    executeAfterValidate = executeAfterValidate || false;

    if ((typeof e !== "undefined") && (e != null)) {
        if (e.keyCode == 13) {
            executeAfterValidate = true;
        }
        var target = $jq(e.target);
        target.addClass(dirtyInput);
    }

    $jq(form + " input, " + form + " textarea").each(function() {
        var el = $jq(this);
        var elemValue = el.val();

        if (executeAfterValidate) {
            el.addClass(dirtyInput);
        }

        if (el.hasClass(dirtyInput)) {
            var requiredField = ((typeof el.data(self.domPrefix + "-required") !== "undefined") ? true : false);
            var requiredEmailField = ((typeof el.data(self.domPrefix + "-required-email") !== "undefined") ? true : false)

            if (requiredField) {
                if (elemValue.length) {
                    el.removeClass(errorInput);
                } else {
                    el.addClass(errorInput);
                }
            }

            if (requiredEmailField) {
                if (elemValue.length) {
                    if (self.emailValidationRexep.test(elemValue)) {
                        el.removeClass(errorInput);
                    } else {
                        el.addClass(errorInput);
                    }
                } else {
                    if (!requiredField) {
                        el.removeClass(errorInput);
                    }
                }
            }
        }
    });


    var formIsValid = true;
    var errorInputs = $jq(form + " ." + errorInput);
    if (errorInputs.length) {
        formIsValid = false;
    }

    if (formIsValid && executeAfterValidate && (form == self.loginForm)) {
        this.clientName = $jq(this.clientNameInput).val();
        this.clientEmail = $jq(this.clientEmailInput).val();
        this.clientCompany = $jq(this.clientCompanyInput).val();
        this.rememberMe = $jq(this.rememberMeInput).prop("checked");

        this.setClientCookie();
        self.reconnectRetry = 1;

        self.connect(true);
    }

    if (formIsValid && executeAfterValidate && (form == self.problemIsNotSolvedForm)) {
        self.saveAndExitButtonClick();
    }

    return formIsValid;

};

IBSBC.prototype.dialogIsFinishedClick = function() {
    if (!this.sockIsConnecting) {
        this.problemIsSolved = null;
        this.problemIsSolvedClick();

        $jq(this.problemIsNotSolvedEmail).val(this.clientEmail);
        $jq(this.problemIsNotSolvedPhone).val("");
        $jq(this.problemIsNotSolvedComment).val("");

        if (this.chatRoomIsClosedByAssisant) {
            $jq(this.backToDialogButton).hide();
        } else {
            $jq(this.backToDialogButton).show();
        }

        $jq(this.loginForm).hide();
        $jq(this.conversationForm).hide();
        $jq(this.dialogIsFinishedForm).show();
    }
};

IBSBC.prototype.problemIsSolvedClick = function() {
    if ((this.problemIsSolved == "yes") || (this.problemIsSolved == null)) {
        $jq(this.problemIsNotSolvedFormData).hide();
    } else {
        $jq(this.problemIsNotSolvedFormData).show();
    }

    if (this.problemIsSolved == null) {
        $jq(this.problemIsSolvedRadio).prop("checked", false);
    }
};


IBSBC.prototype.startConversation = function() {
    if (!this.userIsConnecting && !this.sockIsConnecting) {
        this.clientName = $jq(this.clientNameInput).val();
        this.clientEmail = $jq(this.clientEmailInput).val();
        this.clientCompany = $jq(this.clientCompanyInput).val();
        this.rememberMe = $jq(this.rememberMeInput).prop("checked");

        this.validateForm(null, true, this.loginForm);
    }
};


IBSBC.prototype.sendOfflineMessageButtonClick = function() {
    var self = this;
    if (!self.offlineMessageIsSubmitting) {
        var formIsValid = self.validateForm(null, true, self.sendOfflineMessageForm);

        if (formIsValid) {
            self.clientName = $jq(self.offlineClientNameInput).val();
            self.clientEmail = $jq(self.offlineClientEmailInput).val();
            self.clientCompany = $jq(self.offlineClientCompanyInput).val();
            self.offlineDescription = $jq(self.offlineDescriptionTextarea).val();
            self.rememberMe = false;

            self.setClientCookie();

            self.offlineMessageIsSubmitting = true;
            $jq(self.sendOfflineMessageButton).addClass('ibs-business-chat-disabled');

            var dataToSend = {
                name: self.clientName,
                email: self.clientEmail,
                company: self.clientCompany,
                description: self.offlineDescription,
                widgetId: {"$oid": self.widgetId},
                page: window.location.href,
                trackingCookie: self.trackingCookie
            };

            $jq.ajax({
                type: "POST",
                dataType: "json",
                data: JSON.stringify(dataToSend),
                contentType: "application/json; charset=utf-8",
                url: widgetConfig.baseURL + "user/chat/send_offline_message",
                crossDomain: true
            })
                .success(function (data) {
                    self.offlineMessageIsSubmitting = false;
                    $jq(self.sendOfflineMessageButton).removeClass('ibs-business-chat-disabled');

                    if (data.status && (data.status == "ok")) {
                        $jq(self.offlineDescriptionTextarea).val("");
                        self.postIframeMessage('IBS-Widget-Offline-Message-Successfully-Sent');
                        self.minimizeChatFormIconClick();
                    } else {
                        self.postIframeMessage('IBS-Widget-Ajax-Request-Error-Occurred');
                    }

                })
                .error(function (data, status) {
                    self.offlineMessageIsSubmitting = false;
                    $jq(self.sendOfflineMessageButton).removeClass('ibs-business-chat-disabled');
                    self.postIframeMessage('IBS-Widget-Ajax-Request-Error-Occurred');
                });
        }
    }
};

IBSBC.prototype.composeMessage = function(messages, userType, created) {
    var isUser = userType.isUser || false;
    var isAssistant = userType.isAssistant || false;
    var isSystem = userType.isSystem || false;
    var isGreetingMessage = userType.isGreetingMessage || false;

    var messagesDiv = "";
    for (var i = 0; i < messages.length; i++) {
        messagesDiv += '<div class="' + this.domPrefix + ' ' + this.messageText + ( (i == (messages.length - 1)) ? " " + this.lastMessageLine : "") +'">' + messages[i] + '</div>';
    }

    var usernameClass = this.messageUsernameClass["client"];
    if (isAssistant) usernameClass = this.messageUsernameClass["assistant"];
    if (isSystem) usernameClass = this.messageUsernameClass["system"];
    if (isGreetingMessage) usernameClass = this.messageUsernameClass["greetingMessage"];

    var userName = this.clientName;
    if (isAssistant || isSystem) userName = userType.username;

    var usernameDiv = '<div class="' + this.domPrefix + '-ellipsis ' + this.domPrefix + ' ' + this.domPrefix + '-message-username ' + this.domPrefix + '-message-username-' + usernameClass + '">' + userName + '</div>';

    var messageTime = "";
    if (!_.isNull(created)) {
        messageTime = new Date(created);
        var messageTimeMinutes = messageTime.getMinutes();
        if (messageTimeMinutes < 10) {
            messageTimeMinutes = "0" + messageTimeMinutes
        }
        messageTime = messageTime.getHours() + ":" + messageTimeMinutes;
    }

    var result =
        '<div class="' + this.domPrefix + ' ' + this.messageBlock + ' ' + this.lastMessageBlock + ' ' + this.domPrefix + '-message-author-' + usernameClass + '">' +
        '<div class="' + this.domPrefix + ' ' + this.domPrefix + '-user-photo">' +
        ((isAssistant || isSystem) && userType.avatar && userType.avatar.length ? '<img src="' + userType.avatar + '" title="' + userType.username + '">' : '') +
        '</div>' +
        '<div class="' + this.domPrefix + ' ' + this.domPrefix + '-message-content">' +
        '<div class="' + this.domPrefix + ' ' + this.domPrefix + '-message-details">' +
        (!isGreetingMessage ? '<div class="' + this.domPrefix + ' ' + this.domPrefix + '-message-time">' + messageTime + '</div>' + usernameDiv : '') +
        '</div>' +
        '<div class="' + this.domPrefix + '">' +
        messagesDiv +
        '</div>' +
        '</div>' +
        '</div>';

    return result;
};

IBSBC.prototype.sendMessage = function() {
    if (!this.sockIsConnecting) {
        var message = $jq(this.userMessageTextarea).val().replace(/(\r\n|\n|\r)/gm, "");
        if (message.length) {

            var created = new Date().getTime();
            this.showMessage("client", message, created, {isUser: true});

            $jq(this.userMessageTextarea).val('');

            this.sendSockMessage("UserChatMessage", { message: message, chatRoomId: this.chatRoomId}, created);
        }
    }
};


IBSBC.prototype.sendSockMessage = function(event, content, created) {
    if ((typeof this.sock !== "undefined") && (this.sock != null)) {
        var sockData = content;
        sockData.created = created;
        sockData.event = event;

        //console.log(sockData);
        this.sock.send(JSON.stringify(sockData));
    }
};

IBSBC.prototype.showMessage = function(author, messageContent, created, userType, immediateScroll) {
    if ((this.lastMessageAuthorId != author) || ((created - this.authorMessageGroupTime) > this.messagesGroupInterval)) {
        this.authorMessagesGroup = [];
    }
    if ((this.lastMessageAuthorId != author) || (this.authorMessageGroupTime == null)) {
        this.authorMessageGroupTime = created;
    }

    this.authorMessagesGroup.push(messageContent);

    var composedMessage = this.composeMessage(this.authorMessagesGroup, userType, this.authorMessageGroupTime);

    $jq("." + this.messageText).removeClass(this.lastMessageLine);


    if ((this.lastMessageAuthorId != author) || ((created - this.authorMessageGroupTime) > this.messagesGroupInterval)) {
        $jq("." + this.messageBlock).removeClass(this.lastMessageBlock);
        $jq(this.conversationLog).append(composedMessage);
        this.authorMessageGroupTime = created;
    } else {
        var lastMessageBlock = $jq("." + this.lastMessageBlock);
        if (lastMessageBlock.length > 0) {
            $jq("." + this.lastMessageBlock).replaceWith(composedMessage);
        } else {
            $jq("." + this.messageBlock).removeClass(this.lastMessageBlock);
            $jq(this.conversationLog).append(composedMessage);
        }
    }

    immediateScroll = immediateScroll || false;
    this.scrollToLastMessage(immediateScroll);//
    this.lastMessageAuthorId = author;
};

IBSBC.prototype.composeSystemMessage = function(messageContent, created, immediateScroll) {
    immediateScroll = immediateScroll || false;
    this.showMessage("system", messageContent, created, {isSystem: true, username: "System", avatar: false});
};


IBSBC.prototype.composeAssistantMessage = function(messageContent, created, assistantId, immediateScroll) {
    var assistantName = "System";
    var assistantAvatar = "";
    if (!_.isUndefined(this.companyAssistants[assistantId])) {
        var assistant = this.companyAssistants[assistantId];
        assistantName = assistant.name;
        assistantAvatar = assistant.avatar;
    }

    immediateScroll = immediateScroll || false;
    this.showMessage("assistant" + assistantId, messageContent, created, {isAssistant: true, username: assistantName, avatar: assistantAvatar}, immediateScroll);
};


IBSBC.prototype.scrollToLastMessage = function(immediateScroll) {
    immediateScroll = immediateScroll || false;
    var duration = 100;
    if (immediateScroll) {
        duration = 0;
    }

    var offsetTop = $jq(this.conversationLog).offset().top;
    $jq(this.conversationLog).scrollTo("." + this.lastMessageLine, {offsetTop: offsetTop, duration: duration});
};

IBSBC.prototype.emailTranscriptButtonClick = function() {
    var self = this;
    $jq(self.emailTranscriptButton).prop('disabled', true);

    $jq.ajax({
        type: "POST",
        dataType: "json",
        url: widgetConfig.baseURL + "user/chat/email/"+self.chatRoomId+"/"+self.trackingCookie,
        crossDomain: true
    })
        .success(function () {
            $jq(self.emailTranscriptButton).prop('disabled', false);
            self.postIframeMessage('IBS-Widget-Chat-Transcript-Successfully-Sent');
        })
        .error(function (data, status) {
            if (!_.isUndefined(data.responseJSON) &&  !_.isUndefined(data.responseJSON.status) && (data.responseJSON.status == "error")) {
                if (!_.isUndefined(data.responseJSON.message)) {
                    if (data.responseJSON.message == "chat_room_not_found") {
                        self.postIframeMessage('IBS-Widget-Ajax-Request-Error-Occurred');
                    }
                }
            }

            $jq(self.emailTranscriptButton).prop('disabled', false);
        });
};

IBSBC.prototype.initPlaceholders = function() {
    $jq('input, textarea').placeholder({ customClass: this.domPrefix + '-placeholder' });
};

String.prototype.IBSBCReplaceAll = function(search, replacement) {
    var target = this;
    return target.split(search).join(replacement);
};

IBSBC.prototype.translateWidgetHtml = function(html) {
    _.each(widgetConfig.viewTemplates, function (value, key) {
        html = html.IBSBCReplaceAll('[' + key + ']', value);
    });

    return html;
};


$jq(function() {
    $jq.support.cors = true;
    var domPrefix = "ibs-business-chat";

    var ibsbc = new IBSBC(domPrefix, widgetId);
    widgetHtml = ibsbc.translateWidgetHtml(widgetHtml);
    $jq("body").append(widgetHtml).addClass(domPrefix);;

    ibsbc.init();

    ibsbc.checkCurrentChatRoomId();
    ibsbc.initPlaceholders();
});


