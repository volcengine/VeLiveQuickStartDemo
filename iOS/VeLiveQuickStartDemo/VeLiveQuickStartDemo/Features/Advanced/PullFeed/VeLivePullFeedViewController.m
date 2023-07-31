/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePullFeedViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/7/24.
//

#import "VeLivePullFeedViewController.h"
#import "VeLivePageViewController.h"
#import "VeLivePullFeedItemViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePullFeedViewController () <VeLivePageControllerDelegate, VeLivePageControllerDataSource, VeLivePlayerObserver>
@property (nonatomic, strong) VeLivePageViewController *pageController;
@end

@implementation VeLivePullFeedViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupPageController];
    [self.pageController reloadData];
}


- (void)setupPageController {
    self.view.backgroundColor = [UIColor whiteColor];
    [self addChildViewController:self.pageController];
    UIView *pageView = self.pageController.view;
    pageView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:pageView];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-0-[pageView]-0-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(pageView)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-0-[pageView]-0-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(pageView)]];
}
// MARK:  ---- ATPageViewControllerDataSource & Delegate
- (NSInteger)numberOfItemInPageViewController:(VeLivePageViewController *)pageViewController {
    return self.urlList.count;
}

- (__kindof UIViewController <VeLivePageItemProtocol> *)pageViewController:(VeLivePageViewController *)pageViewController pageForItemAtIndex:(NSUInteger)index {
    VeLivePullFeedItemViewController *itemController = [pageViewController dequeueItemForReuseIdentifier:@"VeLivePullFeedItemViewController"];
    if (!itemController) {
        itemController = [[VeLivePullFeedItemViewController alloc] initWithReuseIdentifier:@"VeLivePullFeedItemViewController"];
    }
    itemController.url = self.urlList[index];
    return itemController;
}

- (BOOL)shouldScrollVertically:(VeLivePageViewController *)pageViewController {
    return YES;
}


- (VeLivePageViewController *)pageController {
    if (!_pageController) {
        _pageController = [[VeLivePageViewController alloc] init];
        _pageController.dataSource = self;
        _pageController.delegate = self;
        _pageController.scrollView.directionalLockEnabled = YES;
        _pageController.scrollView.scrollsToTop = NO;
    }
    return _pageController;
}


- (nullable UIScrollView *)contentScrollViewForEdge:(NSDirectionalRectEdge)edge API_AVAILABLE(ios(15.0),tvos(15.0)) {
    return nil;
}
@end
