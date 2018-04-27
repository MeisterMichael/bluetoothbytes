local hcModule = require "plugin.hcModule"
local json = require "json"
local widget = require( "widget" )

local hcModuleMac = "20:17:03:22:25:10"

--bg
local bg = display.newRect( display.contentCenterX, display.contentCenterY, display.actualContentWidth, display.actualContentHeight )
local title = display.newText( "HC Module Plugin", display.contentCenterX, 50, native.systemFontBold, 20 )
title:setFillColor( 0 )

--request premission for 6.0+

hcModule.init(function ( e )
	print(json.encode(e))
end)

local connectButton
connectButton = widget.newButton( {
    x = display.contentCenterX,
    y = display.contentCenterY-50,
    id = "connect",
    labelColor = { default={ 1, 1, 1 }, over={ 0, 0, 0, 0.5 } },
    label = "Connect",
    onEvent = function ( e )
        if (e.phase == "ended") then
            hcModule.connect(hcModuleMac)
        end
    end
} )