import { Pipe, PipeTransform } from "@angular/core";

@Pipe({name: 'objectToMap'})
export class ObjectToMapPipe implements PipeTransform {
  transform(obj: any): object {
    const keys = Object.keys(obj)
    var map = new Map<string, any>()
    keys.forEach(key => {
      const value = obj[key] 
      if (value != null) {
        map.set(key, value)
      }
    })
    return map;
  }
}