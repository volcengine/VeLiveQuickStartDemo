/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced.PullFeed;

public interface OnViewPagerListener {
    void onInitComplete();

    void onPageDisplay(boolean isNext, int position);

    void onPageRelease(boolean isNext, int position);

    void onPageSelected(int position, boolean isBottom);
}
