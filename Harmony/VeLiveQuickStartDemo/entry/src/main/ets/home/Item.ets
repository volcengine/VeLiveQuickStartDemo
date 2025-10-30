
export class HomeItem {
  public title: string | Resource = '';
  public route: Nullable<string>
  constructor(title: string | Resource = '', route: Nullable<string> = undefined) {
    this.title = title;
    this.route = route;
  }
}

export class HomeSectionItem {
  public title: string | Resource = '';
  public items: HomeItem[] = [];
  constructor(title: string | Resource = '', items: HomeItem[] = []) {
    this.title = title;
    this.items = items;
  }
}