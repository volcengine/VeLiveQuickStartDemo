/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePKViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/30.
//

#import "VeLivePKViewController.h"
#import "VeLiveSDKHelper.h"
#import "VeLivePKAnchorViewController.h"
@interface VeLivePKViewController ()
@property (weak, nonatomic) IBOutlet UILabel *tipLabel;
@property (weak, nonatomic) IBOutlet UITextField *roomIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *userIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *tokenTextField;
@property (weak, nonatomic) IBOutlet UITextField *otherRoomIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *otherTokenTextField;
@property (weak, nonatomic) IBOutlet UIButton *startBtn;
@end

@implementation VeLivePKViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
}

- (IBAction)startBtnClick:(UIButton *)sender {
    if (![self checkParams]) {
        return;
    }
    
    VeLivePKAnchorViewController *vc = [[VeLivePKAnchorViewController alloc] initWithNibName:@"VeLivePKAnchorViewController" bundle:nil];
    vc.roomID = self.roomIdTextField.text;
    vc.userID = self.userIdTextField.text;
    vc.token = self.tokenTextField.text;
    vc.otherRoomID = self.otherRoomIdTextField.text;
    vc.otherRoomToken = self.otherTokenTextField.text;
    [self.navigationController pushViewController:vc animated:YES];
}

- (BOOL)checkParams {
    if (self.roomIdTextField.text <= 0
        || self.userIdTextField.text <= 0
        || self.tokenTextField.text <= 0
        || self.otherRoomIdTextField.text <= 0
        || self.otherTokenTextField.text <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Check Params");
        return NO;
    }
    return YES;
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Interact_PK", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    
    self.tipLabel.text = NSLocalizedString(@"Interact_Link_Tip", nil);
    self.roomIdTextField.text = RTC_ROOM_ID;
    self.userIdTextField.text = RTC_USER_ID;
    self.tokenTextField.text = RTC_USER_TOKEN;
    self.otherRoomIdTextField.text = RTC_OTHER_ROOM_ID;
    self.otherTokenTextField.text = RTC_OTHER_ROOM_TOKEN;
    
    [self.startBtn setTitle:NSLocalizedString(@"Interact_PK_Start", nil) forState:(UIControlStateNormal)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
