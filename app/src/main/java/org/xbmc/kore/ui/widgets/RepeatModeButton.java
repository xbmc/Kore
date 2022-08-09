/*
 * Copyright 2017 Martijn Brekhof. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui.widgets;


import android.content.Context;
import android.util.AttributeSet;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.type.PlayerType;

public class RepeatModeButton extends HighlightButton {
    public enum MODE {
        OFF,
        ONE,
        ALL
    }

    private MODE mode;

    public RepeatModeButton(Context context) {
        super(context);
        setStyle(context);
    }

    public RepeatModeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle(context);
    }

    public RepeatModeButton(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        setStyle(context);
    }

    public void setMode(String mode) {
        if (mode.equals(PlayerType.Repeat.OFF)) {
            setMode(RepeatModeButton.MODE.OFF);
        } else if (mode.equals(PlayerType.Repeat.ONE)) {
            setMode(RepeatModeButton.MODE.ONE);
        } else {
            setMode(RepeatModeButton.MODE.ALL);
        }
    }

    public void setMode(MODE mode) {
        this.mode = mode;

        switch (mode) {
            case OFF:
                setImageResource(R.drawable.ic_repeat_white_24dp);
                setHighlight(false);
                break;
            case ONE:
                setImageResource(R.drawable.ic_repeat_one_white_24dp);
                setHighlight(true);
                break;
            case ALL:
                setImageResource(R.drawable.ic_repeat_white_24dp);
                setHighlight(true);
                break;
        }
    }

    public MODE getMode() {
        return mode;
    }

    private void setStyle(Context context) {
    }
}
