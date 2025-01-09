import { Injectable } from '@angular/core';
import { ChatImageData } from '../model/message.model';
@Injectable({
  providedIn: 'root',
})
export class FileReaderService {
  async readFile(file: File): Promise<ChatImageData> {
    const reader = new FileReader();
    const fileRead = new Promise<ArrayBuffer>((resolve, reject) => {
      reader.onload = (_) => resolve(reader.result as ArrayBuffer);
      reader.onerror = (_) => reject('could not read file');
    });
    reader.readAsArrayBuffer(file);
    const type = file.name.split('.').pop() || '';
    const b = await fileRead;
    return { data: this.arrayBufferToBase64(b), type: type };
  }
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }
}