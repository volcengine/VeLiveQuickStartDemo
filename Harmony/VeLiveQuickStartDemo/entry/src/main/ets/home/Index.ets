import { VeLiveListDataSource } from '../helper/VeLiveListDataSource';
import { VeLiveNavigationBar } from '../helper/VeLiveNavigationBar';
import { VeLiveRouteName } from '../helper/VeLiveRouteName';
import { HomeItem, HomeSectionItem } from './Item';

@Entry
@ComponentV2
struct Index {
  private navStack: NavPathStack = new NavPathStack()
  private dataSource = new VeLiveListDataSource<HomeSectionItem>();

  aboutToAppear(): void {
    let items = [
      new HomeSectionItem($r('app.string.home_basic_features'), [
        new HomeItem($r('app.string.home_live_pull_streaming'), VeLiveRouteName.PULL_BASIC_STREAM),
      ]),
      new HomeSectionItem($r('app.string.home_advanced_features'), [
        new HomeItem($r('app.string.home_picture_in_picture'), VeLiveRouteName.PULL_ADVANCED_PIP),
      ]),
    ]
    this.dataSource.reset(items)
  }

  @Builder
  private buildSection(section: HomeSectionItem, index: number) {
    ListItemGroup({ header: this.buildSectionHeader(section), space: 10 }) {
      LazyForEach(new VeLiveListDataSource<HomeItem>(section.items),
        (item: HomeItem, itemIndex: number) => this.buildSectionItem(section, item, itemIndex))
    }
  }

  @Builder
  private buildSectionHeader(section: HomeSectionItem) {
    Row() {
      Text(section.title).fontColor(Color.Blue).width('100%').fontSize(20).padding(10)
    }
  }

  @Builder
  private buildSectionItem(section: HomeSectionItem, item: HomeItem, index: number) {
    Row() {
      Text(item.title)
        .fontSize(18)
        .fontWeight(FontWeight.Regular)
        .width('100%')
        .backgroundColor($r('app.color.white_color'))
        .padding(10)
        .borderRadius(5)
    }.padding({ left: 10, right: 10 }).onClick((e) => {
      if (item.route != undefined) {
        this.navStack.pushPathByName(item.route, null)
      }
    })
  }

  build() {
    Navigation(this.navStack) {
      Column() {
        VeLiveNavigationBar({title: $r('app.string.home_title'), hideBackIcon: true})
        List() {
          LazyForEach(this.dataSource,
            (sectionItem: HomeSectionItem, index) => this.buildSection(sectionItem, index))
        }
        .width('100%').layoutWeight(1)
      }
      .expandSafeArea()
      .width('100%').height('100%')
      .backgroundColor($r('app.color.view_background'))
    }.hideTitleBar(true)
  }
}