/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

 
 //
//  VeLiveHomeTableViewCell.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import "VeLiveHomeTableViewCell.h"
@interface VeLiveHomeTableViewCell ()
@property (weak, nonatomic) IBOutlet UIView *shadowView;

@end
@implementation VeLiveHomeTableViewCell

- (void)awakeFromNib {
    [super awakeFromNib];
    [self setupShadow];
}

- (void)setupShadow {
    self.shadowView.layer.shadowColor = UIColor.blackColor.CGColor;
    self.shadowView.layer.shadowRadius = 3;
    self.shadowView.layer.shadowOpacity = 0.02;
    self.shadowView.layer.shadowOffset = CGSizeZero;
    self.shadowView.layer.cornerRadius = 10;
    self.shadowView.layer.masksToBounds = NO;
}
@end
