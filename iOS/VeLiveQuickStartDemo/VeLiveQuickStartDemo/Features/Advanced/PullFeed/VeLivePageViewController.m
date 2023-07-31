/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePageViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/7/24.
//

#import "VeLivePageViewController.h"
#import <objc/runtime.h>
NSUInteger const VeLivePageMaxCount = NSIntegerMax;

typedef NS_ENUM(NSInteger, VeLiveTransition) {
    VeLiveTransitionIde = 0,
    VeLiveTransitionAppear = 1 << 1,
    VeLiveTransitionDisAppear = 1 << 2,
};

@interface NSObject (VeLievPageItemAdd)

@property (nonatomic, assign) NSUInteger vel_pageIndex;

@property (nonatomic, assign) VeLiveTransition vel_Transition;

@property (nonatomic, assign) BOOL vel_Appearance;
@end

@implementation UIViewController (VeLievPageItemAdd)

- (void)setVel_pageIndex:(NSUInteger)vel_pageIndex {
    objc_setAssociatedObject(self, @selector(vel_pageIndex), @(vel_pageIndex), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSUInteger)vel_pageIndex {
    return [objc_getAssociatedObject(self, _cmd) unsignedIntegerValue];
}

- (void)setVel_Transition:(VeLiveTransition)vel_Transition {
    objc_setAssociatedObject(self, @selector(vel_Transition), @(vel_Transition), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (VeLiveTransition)vel_Transition {
    return [objc_getAssociatedObject(self, _cmd) integerValue];
}

- (void)setVel_Appearance:(BOOL)vel_Appearance {
    objc_setAssociatedObject(self, @selector(vel_Appearance), @(vel_Appearance), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)vel_Appearance {
    return [objc_getAssociatedObject(self, _cmd) boolValue];
}
@end

static NSString *VeLivePageViewControllerExceptionKey = @"VeLivePageViewControllerExceptionKey";

@interface VeLivePageViewController () <UIScrollViewDelegate>
@property (nonatomic, assign) NSInteger itemCount;
@property (nonatomic, assign) BOOL isVerticalScroll;
@property (nonatomic, assign) BOOL needReloadData;
@property (nonatomic, assign) BOOL shouldChangeToNextPage;
@property (nonatomic, assign) VeLivePageDirection currentDirection;
@property (nonatomic, strong) UIScrollView *scrollView;
@property (nonatomic, strong) NSMutableArray<UIViewController<VeLivePageItemProtocol> *> *viewControllers;
@property (nonatomic, assign) BOOL releaseTouch;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSMutableArray<UIViewController<VeLivePageItemProtocol> *> *> *cacheViewControllers;
@property (nonatomic, strong) UIViewController <VeLivePageItemProtocol> *currentViewController;
@property (nonatomic, strong) UIViewController <VeLivePageItemProtocol> *changeToViewController;
@property (nonatomic, assign) CGFloat lastOffset;
@end

@implementation VeLivePageViewController
- (instancetype)initWithNibName:(nullable NSString *)nibNameOrNil bundle:(nullable NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if(self) {
        [self setupPageController];
    }
    return self;
}

- (nullable instancetype)initWithCoder:(NSCoder *)aDecoder {
    self = [super initWithCoder:aDecoder];
    if(self) {
        [self setupPageController];
    }
    return self;
}

- (void)setupPageController {
    self.viewControllers = [[NSMutableArray alloc] init];
    self.cacheViewControllers = [[NSMutableDictionary alloc] init];
    [self.view addSubview:self.scrollView];
    self.currentIndex = VeLivePageMaxCount;
    self.needReloadData = YES;
}

- (BOOL)shouldAutomaticallyForwardAppearanceMethods {
    return NO;
}

- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    self.scrollView.frame = self.view.bounds;
    [self _reloadDataIfNeeded];
    [self _layoutChildViewControllers];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [self changeTransition:(VeLiveTransitionDisAppear) forViewController:self.currentViewController endAppearance:NO];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self changeTransition:(VeLiveTransitionDisAppear) forViewController:self.currentViewController endAppearance:YES];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self changeTransition:(VeLiveTransitionAppear) forViewController:self.currentViewController endAppearance:NO];
    
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self changeTransition:(VeLiveTransitionAppear) forViewController:self.currentViewController endAppearance:YES];
}

- (void)_layoutChildViewControllers {
    [self.viewControllers enumerateObjectsUsingBlock:^(UIViewController<VeLivePageItemProtocol> * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        obj.view.frame = CGRectMake(obj.view.frame.origin.x, obj.view.frame.origin.y, self.view.frame.size.width, self.view.frame.size.height);
    }];
}

- (void)_reloadDataIfNeeded {
    if (self.needReloadData) {
        [self reloadData];
    }
}

- (void)_clearData {
    [self.viewControllers enumerateObjectsUsingBlock:^(UIViewController<VeLivePageItemProtocol> * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self _veRemoveChildViewController:obj];
    }];
    [self.viewControllers removeAllObjects];
    self.currentDirection = VeLivePageDirectionUnknown;
    self.itemCount = 0;
    self.currentIndex = VeLivePageMaxCount;
}

- (UIViewController<VeLivePageItemProtocol> *)_addChildViewControllerFromDataSourceIndex:(NSUInteger)index transition:(VeLiveTransition)transition {
    UIViewController<VeLivePageItemProtocol> *viewController = [self _childViewControllerAtIndex:index];
    
    [self changeTransition:(transition) forViewController:viewController endAppearance:YES];
    
    if (viewController) return viewController;
    
    viewController = [self.dataSource pageViewController:self pageForItemAtIndex:index];
    if (!viewController) {
        [NSException raise:VeLivePageViewControllerExceptionKey format:@"VeLivePageViewController(%p) pageViewController:pageForItemAtIndex: must return a no nil instance", self];
    }
    viewController.vel_pageIndex = index;
    [self addChildViewController:viewController];
    if (!self.isVerticalScroll) {
        viewController.view.frame = CGRectMake(index * self.view.frame.size.width, 0, self.view.frame.size.width, self.view.frame.size.height);
    } else {
        viewController.view.frame = CGRectMake(0, index * self.view.frame.size.height, self.view.frame.size.width, self.view.frame.size.height);
    }
    [self.scrollView addSubview:viewController.view];
    [viewController didMoveToParentViewController:self];
    
    
    if ([viewController respondsToSelector:@selector(itemControllerDidLoaded)]) {
        [viewController itemControllerDidLoaded];
    }
    
    return viewController;
}

- (void)_veRemoveChildViewController:(UIViewController<VeLivePageItemProtocol> *)removedViewController {
    [removedViewController willMoveToParentViewController:nil];
    [removedViewController.view removeFromSuperview];
    [removedViewController removeFromParentViewController];
    removedViewController.vel_pageIndex = VeLivePageMaxCount;
    if ([removedViewController respondsToSelector:@selector(reuseIdentifier)] && removedViewController.reuseIdentifier.length) {
        NSMutableArray<UIViewController<VeLivePageItemProtocol> *>*reuseViewControllers = [self.cacheViewControllers objectForKey:removedViewController.reuseIdentifier];
        if (!reuseViewControllers) {
            reuseViewControllers = [[NSMutableArray<UIViewController<VeLivePageItemProtocol> *> alloc] init];
            [self.cacheViewControllers setObject:reuseViewControllers forKey:removedViewController.reuseIdentifier];
        }
        if (![reuseViewControllers containsObject:removedViewController]) {
            [reuseViewControllers addObject:removedViewController];
        }
    }
}

- (UIViewController<VeLivePageItemProtocol> *)_childViewControllerAtIndex:(NSUInteger)index {
    __block UIViewController<VeLivePageItemProtocol> *findViewController = nil;
    [self.viewControllers enumerateObjectsUsingBlock:^(UIViewController<VeLivePageItemProtocol> * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (obj.vel_pageIndex == index) {
            findViewController = obj;
        }
    }];
    return findViewController;
}


#pragma mark - Public Methods
- (void)setCurrentIndex:(NSUInteger)currentIndex {
    [self setCurrentIndex:currentIndex autoAdjustOffset:YES];
}

- (void)setCurrentIndex:(NSUInteger)currentIndex autoAdjustOffset:(BOOL)autoAdjustOffset {
    if (_currentIndex == currentIndex) return;
    if (_itemCount == 0) {
        _currentIndex = currentIndex;
        return;
    }
    if (currentIndex > self.itemCount - 1) {
        [NSException raise:VeLivePageViewControllerExceptionKey format:@"VeLivePageViewController(%p) currentIndex out of bounds %lu", self, (unsigned long)currentIndex];
    }
    NSMutableArray *addedViewControllers = [[NSMutableArray alloc] init];
    UIViewController<VeLivePageItemProtocol> *currentVieController = [self _addChildViewControllerFromDataSourceIndex:currentIndex
                                                                                                           transition:(VeLiveTransitionAppear)];
    [addedViewControllers addObject:currentVieController];
    if (currentIndex != 0) {
        UIViewController<VeLivePageItemProtocol> *nextViewController = [self _addChildViewControllerFromDataSourceIndex:currentIndex - 1
                                                                                                             transition:(VeLiveTransitionIde)];
        [addedViewControllers addObject:nextViewController];
    }
    if (self.itemCount > 1 && currentIndex != self.itemCount - 1) {
        UIViewController<VeLivePageItemProtocol> *preViewController = [self _addChildViewControllerFromDataSourceIndex:currentIndex + 1
                                                                                                            transition:VeLiveTransitionIde];
        [addedViewControllers addObject:preViewController];
    }
    
    NSMutableArray *removedViewController = [[NSMutableArray alloc] init];
    [self.viewControllers enumerateObjectsUsingBlock:^(UIViewController<VeLivePageItemProtocol> * _Nonnull vc, NSUInteger idx, BOOL * _Nonnull stop) {
        __block BOOL findVC = NO;
        [addedViewControllers enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            if (vc == obj) {
                findVC = YES;
            }
        }];
        if (!findVC) {
            [removedViewController addObject:vc];
        }
    }];
    [removedViewController enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self _veRemoveChildViewController:obj];
    }];
    [addedViewControllers enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (![self.viewControllers containsObject:obj]) {
            [self.viewControllers addObject:obj];
        }
    }];
    [self.viewControllers removeObjectsInArray:removedViewController];
    UIViewController <VeLivePageItemProtocol> *lastViewController = self.currentViewController;
    _currentIndex = currentIndex;
    self.currentViewController = [self _childViewControllerAtIndex:_currentIndex];
    
    if (autoAdjustOffset) {
        if (!self.isVerticalScroll) {
            self.scrollView.contentOffset = CGPointMake(currentIndex * self.view.frame.size.width, 0);
        } else {
            self.scrollView.contentOffset = CGPointMake(0, currentIndex * self.view.frame.size.height);
        }
        if (self.view.window) {
            [self changeTransition:(VeLiveTransitionDisAppear) forViewController:lastViewController endAppearance:YES];
            [self changeTransition:(VeLiveTransitionAppear) forViewController:self.currentViewController endAppearance:YES];
        }
    }
}


#pragma mark - Variable Setter & Getter
- (UIScrollView *)scrollView {
    if (!_scrollView) {
        _scrollView = [[UIScrollView alloc] init];
        _scrollView.backgroundColor = [UIColor blackColor];
        _scrollView.showsVerticalScrollIndicator = NO;
        _scrollView.showsHorizontalScrollIndicator = NO;
        _scrollView.pagingEnabled = YES;
        _scrollView.directionalLockEnabled = YES;
        _scrollView.delegate = self;
        self.scrollView.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
    }
    return _scrollView;
}

- (void)setDelegate:(id<VeLivePageControllerDelegate>)delegate {
    _delegate = delegate;
}

- (void)setDataSource:(id<VeLivePageControllerDataSource>)dataSource {
    _dataSource = dataSource;
    _needReloadData = YES;
}

- (UIViewController<VeLivePageItemProtocol> *)dequeueItemForReuseIdentifier:(NSString *)reuseIdentifier {
    NSMutableArray<UIViewController<VeLivePageItemProtocol> *> *cacheKeyViewControllers = [self.cacheViewControllers objectForKey:reuseIdentifier];
    if (!cacheKeyViewControllers) return nil;
    UIViewController<VeLivePageItemProtocol> *viewController = [cacheKeyViewControllers firstObject];
    [cacheKeyViewControllers removeObject:viewController];
    if ([viewController respondsToSelector:@selector(itemControllerPrepareForReuse)]) {
        [viewController itemControllerPrepareForReuse];
    }
    return viewController;
}

- (void)reloadData {
    [self reloadDataWithAppearanceTransition:YES];
}

- (void)invalidateLayout {
    [self reloadDataWithAppearanceTransition:NO];
}

- (void)reloadDataWithAppearanceTransition:(BOOL)appearanceTransition {
    self.needReloadData = YES;
    NSInteger preIndex = self.currentIndex;
    [self _clearData];
    if ([_dataSource respondsToSelector:@selector(shouldScrollVertically:)]) {
        self.isVerticalScroll = [self.dataSource shouldScrollVertically:self];
    }
    if ([_dataSource respondsToSelector:@selector(numberOfItemInPageViewController:)]) {
        self.itemCount = [self.dataSource numberOfItemInPageViewController:self];
        if (!self.isVerticalScroll) {
            [self.scrollView setContentSize:CGSizeMake(self.view.frame.size.width * self.itemCount, 0)];
        } else {
            [self.scrollView setContentSize:CGSizeMake(0, self.view.frame.size.height * self.itemCount)];
        }
    }
    if ([_dataSource respondsToSelector:@selector(pageViewController:pageForItemAtIndex:)]) {
        if (preIndex >= _itemCount || preIndex == VeLivePageMaxCount) {
            [self setCurrentIndex:0 autoAdjustOffset:appearanceTransition];
        } else {
            [self setCurrentIndex:preIndex autoAdjustOffset:appearanceTransition];
        }
    }
    self.needReloadData = NO;
}

- (void)reloadContentSize {
    if ([_dataSource respondsToSelector:@selector(numberOfItemInPageViewController:)]) {
        NSInteger preItemCount = self.itemCount;
        self.itemCount = [_dataSource numberOfItemInPageViewController:self];
        if (!self.isVerticalScroll) {
            BOOL resetContentOffset = NO;
            if (preItemCount < self.itemCount) {
                resetContentOffset = YES;
            }
            [self.scrollView setContentSize:CGSizeMake(self.view.frame.size.width * self.itemCount, 0)];
            if (resetContentOffset && self.scrollView.contentOffset.x > self.scrollView.contentSize.width - self.scrollView.frame.size.width) {
                self.scrollView.contentOffset = CGPointMake(self.view.frame.size.width * (self.itemCount - 1), 0);
            }
        } else {
            BOOL resetContentOffset = NO;
            if (preItemCount < self.itemCount) {
                resetContentOffset = YES;
            }
            [self.scrollView setContentSize:CGSizeMake(0, self.view.frame.size.height * self.itemCount)];
            if (resetContentOffset && self.scrollView.contentOffset.y > self.scrollView.contentSize.height - self.scrollView.frame.size.height) {
                self.scrollView.contentOffset = CGPointMake(0, self.view.frame.size.height * (self.itemCount - 1));
            }
        }
    }
}

- (void)_shouldChangeToNextPage {
    UIViewController<VeLivePageItemProtocol> *lastViewController = self.currentViewController;
    CGFloat page = _currentIndex;
    if (self.currentDirection == VeLivePageDirectionNext) {
        page = self.currentIndex + 1;
    } else {
        page = self.currentIndex - 1;
    }
    if (self.isVerticalScroll) {
        page = self.scrollView.contentOffset.y / self.scrollView.frame.size.height + 0.5;
    } else {
        page = self.scrollView.contentOffset.x / self.scrollView.frame.size.width + 0.5;
    }
    if (self.currentDirection == VeLivePageDirectionUnknown) {
        return;
    } else if (self.currentIndex == 0 && self.currentDirection == VeLivePageDirectionPrevious) {
        return;
    } else if (self.currentIndex == (self.itemCount - 1) && self.currentDirection == VeLivePageDirectionNext) {
        return;
    } else {
        [self setCurrentIndex:(NSInteger)page autoAdjustOffset:NO];
    }
    [self _noticeDisplayCurrentController:lastViewController];
    self.shouldChangeToNextPage = NO;
}

- (void)_noticeDisplayCurrentController:(UIViewController <VeLivePageItemProtocol>*)lastViewController {
    if (lastViewController != self.currentViewController) {
        if ([_delegate respondsToSelector:@selector(pageViewController:didEndDisplayItem:)]) {
            [self.delegate pageViewController:self didEndDisplayItem:lastViewController];
        }
        if (lastViewController.vel_Transition != VeLiveTransitionIde) {
            [self changeTransition:VeLiveTransitionDisAppear forViewController:lastViewController endAppearance:YES];
        }
        if (self.currentViewController != VeLiveTransitionIde) {
            [self changeTransition:VeLiveTransitionAppear forViewController:self.currentViewController endAppearance:YES];
        }
        self.scrollView.panGestureRecognizer.enabled = YES;
    } else {
        if (self.changeToViewController.vel_Transition != VeLiveTransitionIde) {
            [self changeTransition:VeLiveTransitionDisAppear forViewController:self.changeToViewController endAppearance:YES];
        }
        if (self.currentViewController.vel_Transition != VeLiveTransitionIde) {
            [self changeTransition:VeLiveTransitionAppear forViewController:self.currentViewController endAppearance:YES];
        }
    }
    self.changeToViewController = nil;

    self.currentDirection = VeLivePageDirectionUnknown;
}

- (void)changeTransition:(VeLiveTransition)transition forViewController:(UIViewController <VeLivePageItemProtocol>*)viewController endAppearance:(BOOL)endAppearance {
    if (viewController == nil) {
        return;
    }
    if (viewController.vel_Transition != transition
        && transition != VeLiveTransitionIde) {
        [viewController beginAppearanceTransition:(transition == VeLiveTransitionAppear) animated:YES];
        viewController.vel_Appearance = YES;
    }
    
    if ((endAppearance || transition == VeLiveTransitionIde) && (viewController.vel_Appearance)) {
        [viewController endAppearanceTransition];
        viewController.vel_Appearance = NO;
    }
    
    viewController.vel_Transition = transition;
}

#pragma mark - UIScrollViewDelegate

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    if (self.needReloadData) return;
    if (self.isVerticalScroll && scrollView.contentOffset.x != 0) return;
    if (!self.isVerticalScroll && scrollView.contentOffset.y != 0) return;
    CGFloat offset = self.isVerticalScroll ? scrollView.contentOffset.y : scrollView.contentOffset.x;
    CGFloat itemWidth = self.isVerticalScroll ? self.view.frame.size.height : self.view.frame.size.width;
    CGFloat offsetABS = offset - itemWidth * self.currentIndex;
    CGFloat progress = fabs(offsetABS) / itemWidth;
    if (offsetABS > 0 && self.currentDirection != VeLivePageDirectionNext) {
        if (self.currentIndex == self.itemCount - 1) {
            return;
        }
        self.currentDirection = VeLivePageDirectionNext;
        if (progress >= 0.0) {
            [self changeTransition:(VeLiveTransitionDisAppear) forViewController:self.currentViewController endAppearance:NO];
            
            UIViewController<VeLivePageItemProtocol> *nextViewController = [self _childViewControllerAtIndex:self.currentIndex + 1];
            [self changeTransition:(VeLiveTransitionAppear) forViewController:nextViewController endAppearance:NO];
            self.changeToViewController = nextViewController;
            
            if ([_delegate respondsToSelector:@selector(pageViewController:willDisplayItem:)]) {
                [self.delegate pageViewController:self willDisplayItem:nextViewController];
            }
        }
    } else if (offsetABS < 0 && self.currentDirection != VeLivePageDirectionPrevious) {
        if (self.currentIndex == 0) {
            return;
        }
        self.currentDirection = VeLivePageDirectionPrevious;
        if (progress >= 0.0) {
            [self changeTransition:(VeLiveTransitionDisAppear) forViewController:self.currentViewController endAppearance:NO];
            
            UIViewController<VeLivePageItemProtocol> *preViewController = [self _childViewControllerAtIndex:self.currentIndex - 1];
            [self changeTransition:(VeLiveTransitionAppear) forViewController:preViewController endAppearance:NO];
            self.changeToViewController = preViewController;
            
            if ([_delegate respondsToSelector:@selector(pageViewController:willDisplayItem:)]) {
                [self.delegate pageViewController:self willDisplayItem:preViewController];
            }
        }
    }
    if ([_delegate respondsToSelector:@selector(pageViewController:didScrollChangeDirection:offsetProgress:)]) {
        [self.delegate pageViewController:self didScrollChangeDirection:self.currentDirection offsetProgress:(progress > 1) ? 1 : progress];
    }
    if (progress < 0.0) {
        [self changeTransition:(VeLiveTransitionIde) forViewController:self.currentViewController endAppearance:YES];
        [self changeTransition:(VeLiveTransitionIde) forViewController:self.changeToViewController endAppearance:YES];
        self.currentDirection = VeLivePageDirectionUnknown;
    }
    if (progress >= 1.0) {
        self.shouldChangeToNextPage = YES;
        if (progress > 1 && self.shouldChangeToNextPage) {
            [self _shouldChangeToNextPage];
        }
    }
}

- (void)scrollViewWillEndDragging:(UIScrollView *)scrollView withVelocity:(CGPoint)velocity targetContentOffset:(inout CGPoint *)targetContentOffset {
    CGPoint targetOffset = *targetContentOffset;
    CGFloat offset;
    CGFloat itemLength;
    if (!self.isVerticalScroll) {
        offset = targetOffset.x;
        itemLength = self.view.frame.size.width;
    } else {
        offset = targetOffset.y;
        itemLength = self.view.frame.size.height;
    }
    NSUInteger idx = offset / itemLength;
    UIViewController<VeLivePageItemProtocol> *targetVC = [self _childViewControllerAtIndex:idx];
    if (targetVC != self.currentViewController) {
        if (targetVC.vel_Transition != VeLiveTransitionAppear) { // fix unpair case
            scrollView.panGestureRecognizer.enabled = NO;
            [self changeTransition:(VeLiveTransitionAppear) forViewController:targetVC endAppearance:YES];
        }
    }
}

//- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView {
//    [self.viewControllers enumerateObjectsUsingBlock:^(UIViewController<VeLivePageItemProtocol> * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
//        if (obj.vel_Transition != VeLiveTransitionIde) {
//            [self changeTransition:(VeLiveTransitionIde) forViewController:obj endAppearance:YES];
//        }
//    }];
//}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
    if (!decelerate) {
        [self scrollViewDidStopScroll];
    }
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView {
    [self scrollViewDidStopScroll];
}

- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView {
    [self scrollViewDidStopScroll];
}

- (void)scrollViewDidStopScroll {
    if (self.shouldChangeToNextPage) {
        [self _shouldChangeToNextPage];
    } else {
        [self _noticeDisplayCurrentController:self.currentViewController];
    }
}

@end
