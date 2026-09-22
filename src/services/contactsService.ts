import { TrustedContact } from '../types';

const CONTACTS_KEY = 'saferescue_trusted_contacts';

const DEFAULT_CONTACTS: TrustedContact[] = [
  {
    id: 'contact-1',
    name: 'Sarah Connor',
    phone: '+1 555-0142',
    verified: true,
    relationship: 'Emergency Primary',
    createdAtMillis: Date.now() - 86400000 * 5,
  },
  {
    id: 'contact-2',
    name: 'David Miller',
    phone: '+1 555-0188',
    verified: false,
    relationship: 'Family',
    createdAtMillis: Date.now() - 86400000 * 2,
  },
];

class ContactsService {
  getContacts(): TrustedContact[] {
    try {
      const stored = localStorage.getItem(CONTACTS_KEY);
      if (stored) return JSON.parse(stored);
    } catch {
      // ignore
    }
    this.saveContacts(DEFAULT_CONTACTS);
    return DEFAULT_CONTACTS;
  }

  saveContacts(contacts: TrustedContact[]) {
    localStorage.setItem(CONTACTS_KEY, JSON.stringify(contacts));
  }

  addOrUpdate(name: string, phone: string, id?: string): { success: boolean; contacts: TrustedContact[]; error?: string } {
    const list = this.getContacts();
    if (!id && list.length >= 3) {
      return { success: false, contacts: list, error: 'Maximum of 3 trusted contacts allowed.' };
    }

    if (id) {
      const idx = list.findIndex((c) => c.id === id);
      if (idx >= 0) {
        list[idx] = {
          ...list[idx],
          name: name.trim(),
          phone: phone.trim(),
          verified: false, // Reset verification on edit per Android spec
        };
      }
    } else {
      list.push({
        id: `contact-${Date.now()}`,
        name: name.trim(),
        phone: phone.trim(),
        verified: false,
        createdAtMillis: Date.now(),
      });
    }

    this.saveContacts(list);
    return { success: true, contacts: list };
  }

  toggleVerified(id: string): TrustedContact[] {
    const list = this.getContacts();
    const target = list.find((c) => c.id === id);
    if (target) {
      target.verified = !target.verified;
      this.saveContacts(list);
    }
    return [...list];
  }

  remove(id: string): TrustedContact[] {
    const list = this.getContacts().filter((c) => c.id !== id);
    this.saveContacts(list);
    return list;
  }
}

export const contactsService = new ContactsService();
