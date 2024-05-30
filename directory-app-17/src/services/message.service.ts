import { Injectable } from '@angular/core';

export enum MessageType {
    info, error, success, warning
}

export interface Message {
    type: MessageType
    text: string
}

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  messages: Message[] = [];

  info(text: string) {
    this.add({type: MessageType.info, text: text})
  }

  error(text: string) {
    this.add({type: MessageType.error, text: text})
  }

  success(text: string) {
    this.add({type: MessageType.success, text: text})
  }

  warning(text: string) {
    this.add({type: MessageType.warning, text: text})
  }

  add(message: Message) {
    this.messages.push(message);
  }

  remove (message: Message) {
    this.messages = this.messages.filter(obj => obj !== message);
  }

  clear() {
    this.messages = [];
  }
}