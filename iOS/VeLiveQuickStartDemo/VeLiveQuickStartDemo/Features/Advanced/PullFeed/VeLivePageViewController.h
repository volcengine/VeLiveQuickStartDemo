/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePageViewController.h
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/7/24.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, VeLivePageDirection) {
    VeLivePageDirectionUnknown,
    VeLivePageDirectionPrevious,
    VeLivePageDirectionNext
};

@class VeLivePageViewController;

@protocol VeLivePageItemProtocol <NSObject>
@property (nonatomic, copy) NSString *reuseIdentifier;
- (instancetype)initWithReuseIdentifier:(NSString *)reuseIdentifier;

@optional

- (void)itemControllerPrepareForReuse;

- (void)itemControllerDidLoaded;

@end

@protocol VeLivePageControllerDataSource <NSObject>

@required

- (__kindof UIViewController<VeLivePageItemProtocol> *)pageViewController:(VeLivePageViewController *)pageViewController
                                           pageForItemAtIndex:(NSUInteger)index;

- (NSInteger)numberOfItemInPageViewController:(VeLivePageViewController *)pageViewController;

@optional
- (BOOL)shouldScrollVertically:(VeLivePageViewController *)pageViewController;

@end

@protocol VeLivePageControllerDelegate <NSObject>

@optional

- (void)pageViewController:(VeLivePageViewController *)pageViewController
  didScrollChangeDirection:(VeLivePageDirection)direction
            offsetProgress:(CGFloat)progress;

- (void)pageViewController:(VeLivePageViewController *)pageViewController
           willDisplayItem:(id<VeLivePageItemProtocol>)viewController;

- (void)pageViewController:(VeLivePageViewController *)pageViewController
         didEndDisplayItem:(id<VeLivePageItemProtocol>)viewController;

@end

@interface VeLivePageViewController : UIViewController

@property (nonatomic, assign) NSUInteger currentIndex;

@property (nonatomic, weak) id<VeLivePageControllerDelegate> delegate;

@property (nonatomic, weak) id<VeLivePageControllerDataSource> dataSource;

- (UIScrollView *)scrollView;

- (__kindof UIViewController<VeLivePageItemProtocol> *)dequeueItemForReuseIdentifier:(NSString *)reuseIdentifier;

- (void)reloadData;

- (void)invalidateLayout;

- (void)reloadContentSize;

@end

NS_ASSUME_NONNULL_END
