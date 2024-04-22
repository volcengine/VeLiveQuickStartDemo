/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  ViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2022/10/18.
//
/*
 本文件展示SDK功能入口，只是UI展示无SDK具体功能。
 SDK 具体功能请查看Features 目录下相关能力
 */
#import "ViewController.h"
#import "VeLiveHomeTableViewCell.h"
#import "VeLiveHomeTableHeaderView.h"
#import "VeLiveSDKHelper.h"
@interface ViewController () <UITableViewDelegate, UITableViewDataSource>
@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (weak, nonatomic) IBOutlet UILabel *versionLabel;
@property (nonatomic, strong) NSArray <NSDictionary *> *sections;
@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupSectionData];
    [self setupTableView];
    self.versionLabel.text = [TTSDKManager SDKVersionString];
}

- (void)setupSectionData {
    self.sections = @[
        @{
            @"title" : NSLocalizedString(@"Home_Basic_Features", nil),
            @"items" : @[
                @{
                    @"title" : NSLocalizedString(@"Home_Camera_Push", nil),
                    @"target" : @"VeLivePushCameraViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_Live_Pull_Streaming", nil),
                    @"target" : @"VeLivePullStreamViewController"
                }
            ]
        },
        @{
            @"title" : NSLocalizedString(@"Home_Advanced_Features", nil),
            @"items" : @[
                @{
                    @"title" : NSLocalizedString(@"Home_Live_Beauty_Filter", nil),
                    @"target" : @"VeLivePushBeautyViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_RTM_Push_Streaming", nil),
                    @"target" : @"VeLivePushRTMViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_RTM_Pull_Streaming", nil),
                    @"target" : @"VeLivePullRTMViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_Custom_Push_Stream", nil),
                    @"target" : @"VeLivePushCustomViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_Push_Streaming_Bitrate_Adaptive", nil),
                    @"target" : @"VeLivePushAutoBitrateViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_H265_Hardcoded", nil),
                    @"target" : @"VeLivePush265CodecViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_Picture_In_Picture", nil),
                    @"target" : @"VeLivePictureInPictureViewController"
                },
            ]
        },
        @{
            @"title" : NSLocalizedString(@"Home_Interactive_Features", nil),
            @"items" : @[
                @{
                    @"title" : NSLocalizedString(@"Home_Anchor_And_Audience_Mic", nil),
                    @"target" : @"VeLiveLinkViewController"
                },
                @{
                    @"title" : NSLocalizedString(@"Home_Anchor_VS_Anchor_Pk", nil),
                    @"target" : @"VeLivePKViewController"
                }
            ]
        }
    ];
}

- (void)setupTableView {
    UINib *header = [UINib nibWithNibName:@"VeLiveHomeTableHeaderView" bundle:nil];
    [self.tableView registerNib:header forHeaderFooterViewReuseIdentifier:@"VeLiveHomeTableHeaderView"];
}

- (void)pushToFeatureTarget:(NSString *)target {
    Class class = NSClassFromString(target);
    if (class) {
        id controller = [[class alloc] initWithNibName:target bundle:nil];
        if (controller) {
            [self.navigationController pushViewController:controller animated:true];
        }
    }
}

// MARK: - UITableViewDataSource
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return self.sections.count;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    NSArray *items = [self.sections[section] objectForKey:@"items"];
    return items.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    VeLiveHomeTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"VeLiveHomeTableViewCell"];
    NSArray *items = [self.sections[indexPath.section] objectForKey:@"items"];
    cell.nameLabel.text = [items[indexPath.row] objectForKey:@"title"];
    return cell;
}

// MARK: - UITableViewDelegate
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    NSArray *section = [self.sections[indexPath.section] objectForKey:@"items"];
    NSString *target = [section[indexPath.row] objectForKey:@"target"];
    [self pushToFeatureTarget:target];
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section {
    VeLiveHomeTableHeaderView *header = [tableView dequeueReusableHeaderFooterViewWithIdentifier:@"VeLiveHomeTableHeaderView"];
    header.nameLabel.text = [self.sections[section] objectForKey:@"title"];
    return header;
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section {
    return 30;
}

- (UIView *)tableView:(UITableView *)tableView viewForFooterInSection:(NSInteger)section {
    return [[UIView alloc] initWithFrame:CGRectMake(0, 0, tableView.bounds.size.width, 0.1)];
}

- (CGFloat)tableView:(UITableView *)tableView heightForFooterInSection:(NSInteger)section {
    return 0.1;
}

@end
