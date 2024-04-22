/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveLinkViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/30.
//

#import "VeLiveLinkViewController.h"
#import "VeLiveSDKHelper.h"
#import "VeLiveLinkAnchorViewController.h"
#import "VeLiveLinkAudienceViewController.h"
@interface VeLiveLinkViewController ()
@property (weak, nonatomic) IBOutlet UILabel *tipLabel;
@property (weak, nonatomic) IBOutlet UITextField *roomIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *userIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *tokenTextField;
@property (weak, nonatomic) IBOutlet UIButton *archorBtn;
@property (weak, nonatomic) IBOutlet UIButton *audienceBtn;

@end

@implementation VeLiveLinkViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
}

- (IBAction)anchorBtnClick:(UIButton *)sender {
    if (![self checkParams]) {
        return;
    }
    VeLiveLinkAnchorViewController *vc = [[VeLiveLinkAnchorViewController alloc] initWithNibName:@"VeLiveLinkAnchorViewController" bundle:nil];
    vc.roomID = self.roomIdTextField.text;
    vc.userID = self.userIdTextField.text;
    vc.token = [[VeLiveRTCTokenMaker shareMaker] genDefaultTokenWithRoomID:vc.roomID userId:vc.userID];
    [self.navigationController pushViewController:vc animated:YES];
}

- (IBAction)audienceBtnClick:(UIButton *)sender {
    if (![self checkParams]) {
        return;
    }
    VeLiveLinkAudienceViewController *vc = [[VeLiveLinkAudienceViewController alloc] initWithNibName:@"VeLiveLinkAudienceViewController" bundle:nil];
    vc.roomID = self.roomIdTextField.text;
    vc.userID = self.userIdTextField.text;
    vc.token = [[VeLiveRTCTokenMaker shareMaker] genDefaultTokenWithRoomID:vc.roomID userId:vc.userID];
    [self.navigationController pushViewController:vc animated:YES];
}

- (BOOL)checkParams {
    if (self.roomIdTextField.text <= 0
        || self.userIdTextField.text <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Check Params");
        return NO;
    }
    return YES;
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Interact_Link", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    
    self.tipLabel.text = NSLocalizedString(@"Interact_Link_Tip", nil);
    
    [self.archorBtn setTitle:NSLocalizedString(@"Interact_Link_Anchor", nil) forState:(UIControlStateNormal)];
    [self.audienceBtn setTitle:NSLocalizedString(@"Interact_Link_Audience", nil) forState:(UIControlStateNormal)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
