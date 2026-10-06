import { TrustedContact } from '../types';
import { supabase } from '../lib/supabase';

interface DbContact {
  id: string;
  user_id: string;
  name: string;
  phone: string;
  relationship: string | null;
  verified: boolean;
  created_at: string;
}

function toTrustedContact(row: DbContact): TrustedContact {
  return {
    id: row.id,
    name: row.name,
    phone: row.phone,
    verified: row.verified,
    relationship: row.relationship || undefined,
    createdAtMillis: new Date(row.created_at).getTime(),
  };
}

class ContactsService {
  async getContacts(): Promise<TrustedContact[]> {
    const { data, error } = await supabase
      .from('trusted_contacts')
      .select('*')
      .order('created_at', { ascending: true });

    if (error) {
      console.warn('Failed to load contacts:', error.message);
      return [];
    }

    return (data as DbContact[]).map(toTrustedContact);
  }

  async addOrUpdate(name: string, phone: string, id?: string): Promise<{ success: boolean; contacts: TrustedContact[]; error?: string }> {
    if (id) {
      const { error } = await supabase
        .from('trusted_contacts')
        .update({
          name: name.trim(),
          phone: phone.trim(),
          verified: false,
        })
        .eq('id', id);

      if (error) {
        return { success: false, contacts: [], error: error.message };
      }
    } else {
      const existing = await this.getContacts();
      if (existing.length >= 3) {
        return { success: false, contacts: existing, error: 'Maximum of 3 trusted contacts allowed.' };
      }

      const { error } = await supabase
        .from('trusted_contacts')
        .insert({
          name: name.trim(),
          phone: phone.trim(),
          verified: false,
        });

      if (error) {
        return { success: false, contacts: existing, error: error.message };
      }
    }

    const contacts = await this.getContacts();
    return { success: true, contacts };
  }

  async toggleVerified(id: string): Promise<TrustedContact[]> {
    const contacts = await this.getContacts();
    const target = contacts.find((c) => c.id === id);
    if (target) {
      await supabase
        .from('trusted_contacts')
        .update({ verified: !target.verified })
        .eq('id', id);
    }
    return this.getContacts();
  }

  async remove(id: string): Promise<TrustedContact[]> {
    await supabase
      .from('trusted_contacts')
      .delete()
      .eq('id', id);

    return this.getContacts();
  }
}

export const contactsService = new ContactsService();
