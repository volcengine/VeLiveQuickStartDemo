export class VeLiveListDataSource<T> implements IDataSource {
  private listeners: DataChangeListener[] = []
  private data: T[] = []

  constructor(data: Nullable<T[]> = undefined) {
    if (data != undefined) {
      this.data = data;
      this.notifyDataReload();
    }
  }

  reset(data: Nullable<T[]>) {
    this.data.splice(0, this.data.length)
    if (data != undefined) {
      this.data.push(...data)
    }
    this.notifyDataReload();
  }

  add(data: T): number {
    this.data.push(data)
    this.notifyDataAdd(this.data.length - 1);
    return this.data.length - 1;
  }

  insert(data: T, at: number): number {
    if (at < 0) {
      return -1;
    }
    if (at >= this.data.length) {
      return this.add(data)
    }
    this.data.splice(at, 0, data);
    this.notifyDataAdd(at);
    return at;
  }

  remove(data: T): Nullable<T> {
    const pos = this.data.indexOf(data);
    let deleteItem: Nullable<T> = undefined;
    if (pos >= 0) {
      deleteItem = this.data.splice(pos, 1).shift();
      this.notifyDataDelete(pos);
    }
    return deleteItem;
  }

  removeAt(index: number): Nullable<T> {
    if (index >= 0 && index < this.data.length) {
      return this.remove(this.data[index]);
    }
    return undefined;
  }

  public getData(index: number): T {
    return this.data[index];
  }

  public totalCount(): number {
    return this.data.length;
  }

  registerDataChangeListener(listener: DataChangeListener): void {
    if (this.listeners.indexOf(listener) != -1) {
      this.listeners.push(listener);
    }
  }

  unregisterDataChangeListener(listener: DataChangeListener): void {
    const pos = this.listeners.indexOf(listener);
    if (pos >= 0) {
      this.listeners.splice(pos, 1);
    }
  }

  private notifyDataDelete(index: number): void {
    this.listeners.forEach(listener => {
      listener.onDataDelete(index);
    });
  }

  private notifyDataAdd(index: number): void {
    this.listeners.forEach(listener => {
      listener.onDataAdd(index);
    });
  }

  private notifyDataReload(): void {
    this.listeners.forEach(listener => {
      listener.onDataReloaded();
    });
  }
}