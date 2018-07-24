/*
 * Copyright 2016 Martijn Brekhof. All rights reserved.
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

import android.database.Cursor;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.CursorMatchers;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.xbmc.kore.ui.widgets.HighlightButton;
import org.xbmc.kore.ui.widgets.RepeatModeButton;
import org.xbmc.kore.utils.UIUtils;

public class Matchers {
    public static MenuItemTitleMatcher withMenuTitle(String title) {
        return new MenuItemTitleMatcher(title);
    }

    public static class MenuItemTitleMatcher extends BaseMatcher<Object> {
        private final String title;
        public MenuItemTitleMatcher(String title) { this.title = title; }

        @Override
        public boolean matches(Object o) {
            if (o instanceof MenuItem) {
                return ((MenuItem) o).getTitle().equals(title);
            }
            return false;
        }
        @Override
        public void describeTo(Description description) { }
    }

    public static Matcher<View> withListSize(final int size) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(final View view) {
                if (!(view instanceof  ViewGroup))
                    return false;

                return ((ViewGroup) view).getChildCount() == size;
            }

            @Override public void describeTo(final Description description) {
                description.appendText("List should have " + size + " item(s)");
            }
        };
    }

    public static Matcher<View> withRecyclerViewSize(final int size) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                return (view instanceof RecyclerView) &&
                        ((RecyclerView) view).getAdapter().getItemCount() == size;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView should have " + size + " item(s)");
            }
        };
    }

    public static Matcher<View> withOnlyMatchingDataItems(final Matcher<View> dataMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView))
                    return false;

                RecyclerView recyclerView = (RecyclerView) view;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                    if (! dataMatcher.matches(viewHolder.itemView)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("withOnlyMatchingDataItems: ");
                dataMatcher.describeTo(description);
            }
        };
    }

    public static Matcher<Object> withItemContent(final Matcher<String> textMatcher) {
        return new BoundedMatcher<Object, Cursor>(Cursor.class) {
            @Override
            protected boolean matchesSafely(Cursor item) {
                for (int i = 0; i < item.getColumnCount();i++) {
                    switch (item.getType(i)) {
                        case Cursor.FIELD_TYPE_STRING:
                            if (CursorMatchers.withRowString(i, textMatcher).matches(item))
                                return true;
                            break;
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("withItemContent: ");
                textMatcher.describeTo(description);
            }
        };
    }

    public static Matcher<View> withProgress(final int progress) {
        return new BoundedMatcher<View, SeekBar>(SeekBar.class) {
            @Override
            protected boolean matchesSafely(SeekBar item) {
                return item.getProgress() == progress;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected: " + progress);
            }
        };
    }

    public static Matcher<View> withProgress(final String progress) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            protected boolean matchesSafely(TextView item) {
                return progress.contentEquals(item.getText());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected: " + progress);
            }
        };
    }

    public static Matcher<View> withProgressGreaterThanOrEqual(final String time) {
        return new BoundedMatcher<View, SeekBar>(SeekBar.class) {
            @Override
            protected boolean matchesSafely(SeekBar item) {
                return item.getProgress() >= UIUtils.timeToSeconds(time);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected progress greater than " + time);
            }
        };
    }

    public static Matcher<View> withProgressGreaterThan(final int progress) {
        return new BoundedMatcher<View, SeekBar>(SeekBar.class) {
            @Override
            protected boolean matchesSafely(SeekBar item) {
                return item.getProgress() > progress;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected progress greater than " + progress);
            }
        };
    }

    public static Matcher<View> withHighlightState(final boolean highlight) {
        return new BoundedMatcher<View, HighlightButton>(HighlightButton.class) {
            @Override
            protected boolean matchesSafely(HighlightButton item) {
                return item.isHighlighted();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected: " + highlight);
            }
        };
    }

    public static Matcher<View> withRepeatMode(final RepeatModeButton.MODE mode) {
        return new BoundedMatcher<View, RepeatModeButton>(RepeatModeButton.class) {
            @Override
            protected boolean matchesSafely(RepeatModeButton item) {
                return item.getMode() == mode;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected: " + mode.name());
            }
        };
    }
}
