/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.solution.FloatingWindow;

import java.util.HashMap;
import java.util.Map;

public class VeLiveRefMgr {
    public interface IObject {
        default Object getId() { return this; } // 唯一标识，默认为对象本身
        void onDecRef();
    }
    private static final Map<Object, Integer> mRefMap = new HashMap<>();
    public synchronized static void addRef(IObject livePlayer) {
        if (livePlayer == null) {
            return;
        }
        int refCnt = 1;
        if (mRefMap.containsKey(livePlayer.getId())) {
            refCnt += mRefMap.get(livePlayer.getId());
        }
        mRefMap.put(livePlayer.getId(), refCnt);
    }

    public synchronized static void decRef(IObject livePlayer) {
        if (livePlayer == null || !mRefMap.containsKey(livePlayer.getId())) {
            return;
        }
        int refCnt = mRefMap.get(livePlayer.getId()) - 1;
        if (refCnt == 0) {
            livePlayer.onDecRef();
        } else {
            mRefMap.put(livePlayer.getId(), refCnt);
        }
    }
}