@(
  widgetId:String,
  widgetConfig:play.api.libs.json.JsObject
)(content: JavaScript)
@includeScript(filePath:String) = @{

    import play.api.Play.current

    play.api.Play.resourceAsStream(filePath) match {
        case Some(is) => scala.io.Source.fromInputStream(is).getLines().mkString("\n")
        case _ => ""
    }
}
/* IBS  Business chat v-1 */
@JavaScript( includeScript("public/javascripts/ui/jquery-1.11.js") )


( function( $jq, widgetId, widgetConfig ){

    @JavaScript( includeScript("public/javascripts/ui/jquery.cookie.js") )

    @JavaScript( includeScript("public/javascripts/ui/socks.js") )

    @JavaScript( includeScript("public/javascripts/ui/scrollTo.js") )
    @JavaScript( includeScript("public/javascripts/ui/jquery.pulse.min.js") )
    @JavaScript( includeScript("public/javascripts/ui/underscore-min.js") )
    @JavaScript( includeScript("public/javascripts/ui/soundmanager2.min.js") )
    @JavaScript( includeScript("public/javascripts/ui/jquery.ba-postmessage.min.js") )
    @JavaScript( includeScript("public/javascripts/ui/jquery.placeholder.js") )

    var widgetHtml = "@JavaScriptFormat.escape( views.html.Widget.template().toString )";

    // widget
    @content

} )( jQuery.noConflict(true), "@widgetId", @JavaScript(widgetConfig.toString) );
