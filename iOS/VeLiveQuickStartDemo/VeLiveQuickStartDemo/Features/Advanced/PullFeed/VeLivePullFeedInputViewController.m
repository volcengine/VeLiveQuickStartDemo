/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePullFeedInputViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/7/25.
//

#import "VeLivePullFeedInputViewController.h"
#import "VeLivePullFeedViewController.h"

@interface VeLivePullFeedInputViewController ()
@property (weak, nonatomic) IBOutlet UITextView *urlTextView;
@property (weak, nonatomic) IBOutlet UIButton *startBtn;

@end

@implementation VeLivePullFeedInputViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.urlTextView.text = NSLocalizedString(@"input_url_list", nil);
}

- (IBAction)startBtnClick:(UIButton *)sender {
    VeLivePullFeedViewController *vc = [[VeLivePullFeedViewController alloc] initWithNibName:@"VeLivePullFeedViewController" bundle:nil];
    NSMutableArray *array = [NSMutableArray arrayWithCapacity:30];
    [[self.urlTextView.text componentsSeparatedByString:@"\n"] enumerateObjectsUsingBlock:^(NSString * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSString *url = [obj stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
        if (url.length > 0 && [NSURL URLWithString:url] != nil) {
            [array addObject:url];
        }
    }];
    vc.urlList = array.copy;
    [self.navigationController pushViewController:vc animated:YES];
}

@end
