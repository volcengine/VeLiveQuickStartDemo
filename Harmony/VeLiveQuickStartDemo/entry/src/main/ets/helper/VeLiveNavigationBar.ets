export declare type MenuClickEvent = (event?: ClickEvent) => void

export const VeLiveNavigationBarHeight = 44;

@ComponentV2
export struct VeLiveNavigationBar {
  private navBarHeight: number = VeLiveNavigationBarHeight
  @Event backEvent?: MenuClickEvent = undefined
  @Param navStack?: NavPathStack = undefined
  @Param statusBarBgColor: ResourceColor = Color.Transparent
  @Param navBarBgColor: ResourceColor = Color.Transparent
  @Param title?: ResourceStr = undefined
  @Param hideBackIcon: boolean = false
  @Param tintColor: ResourceColor = Color.Black
  @Param backBtnIcon: PixelMap | ResourceStr | DrawableDescriptor = $r('sys.media.ohos_ic_public_arrow_left')

  @Computed
  get backImageWidth(): number {
    return this.hideBackIcon ? 0 : 30;
  }

  private onLeftBtnClick(event: Nullable<ClickEvent>) {
    if (this.backEvent !== undefined) {
      this.backEvent(event)
    } else if (this.navStack !== undefined) {
      this.navStack.pop()
    }
  }

  build() {
    Stack() {
      Row() {
        if (!this.hideBackIcon) {
          Blank().width(8)
          Image(this.backBtnIcon)
            .fillColor((this.tintColor))
            .height(this.backImageWidth)
            .width(this.backImageWidth)
            .onClick((event) => this.onLeftBtnClick(event))
        }
        if (this.title != undefined) {
          Text(this.title)
            .fontSize(18)
            .fontColor(this.tintColor)
            .fontWeight(FontWeight.Bold)
            .textAlign(TextAlign.Center)
            .layoutWeight(1)
        }
        if (!this.hideBackIcon) {
          Blank().width(this.backImageWidth + 8)
        }
      }
      .backgroundColor(this.navBarBgColor)
      .size({ height: this.navBarHeight, width: '100%' })
      .alignItems(VerticalAlign.Center)
      .justifyContent(FlexAlign.Start)
    }
    .expandSafeArea()
    .backgroundColor(this.statusBarBgColor)
    .zIndex(10000)
  }
}