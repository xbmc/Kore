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

package org.xbmc.kore.testhelpers;

import org.xbmc.kore.testutils.tcpserver.handlers.InputHandler;

import static junit.framework.Assert.assertTrue;
import static org.xbmc.kore.tests.ui.AbstractTestClass.getInputHandler;

public class TestUtils {
    /**
     * Tests if the event received at the server matches the given
     * method name and action
     * @param methodName name of the method that should be received serverside.
     * @param executeAction name of the action that should be received serverside. May be null if the input does not specify an action.
     */
    public static void testHTTPEvent(String methodName, String executeAction) {
        InputHandler inputHandler = getInputHandler();
        assertTrue(inputHandler != null);

        String methodNameReceived = inputHandler.getMethodName();
        assertTrue(methodNameReceived != null);
        assertTrue(methodNameReceived.contentEquals(methodName));

        if (executeAction != null) {
            String actionReceived = inputHandler.getAction();
            assertTrue(actionReceived != null);
            assertTrue(actionReceived.contentEquals(executeAction));
        }
    }
}
