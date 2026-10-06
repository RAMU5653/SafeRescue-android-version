import React, { useState, useEffect, useCallback } from 'react';
import {
  Users,
  UserPlus,
  Edit2,
  Trash2,
  Bell,
  Navigation,
  RefreshCw,
  Sun,
  Shield,
  MessageSquare,
  Send,
} from 'lucide-react';
import { LocationPoint, SafetyPlace, TrustedContact, WeatherCondition } from '../types';
import { contactsService } from '../services/contactsService';
import { fetchNearbySafePlaces, fetchCurrentWeather } from '../services/safetyService';
import { InteractiveSafeMap } from './InteractiveSafeMap';
import { localAiEngine } from '../services/localAiEngine';

interface SafetyTabProps {
  latestLocation: LocationPoint | null;
  onShowAlertNotification: (msg: string) => void;
  onOpenSendMessageModal?: (contact?: TrustedContact, mode?: 'emergency' | 'test') => void;
}

export const SafetyTab: React.FC<SafetyTabProps> = ({
  latestLocation,
  onShowAlertNotification,
  onOpenSendMessageModal,
}) => {
  const [contacts, setContacts] = useState<TrustedContact[]>([]);
  const [editingContact, setEditingContact] = useState<TrustedContact | null>(null);
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [formName, setFormName] = useState('');
  const [formPhone, setFormPhone] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState<TrustedContact | null>(null);
  const [saving, setSaving] = useState(false);

  const [places, setPlaces] = useState<SafetyPlace[]>([]);
  const [weather, setWeather] = useState<WeatherCondition | null>(null);
  const [loadingGuidance, setLoadingGuidance] = useState(false);
  const [guidanceError, setGuidanceError] = useState<string | null>(null);

  const loadContacts = useCallback(async () => {
    const list = await contactsService.getContacts();
    setContacts(list);
  }, []);

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const loadGuidance = useCallback(async () => {
    if (!latestLocation) return;
    setLoadingGuidance(true);
    setGuidanceError(null);
    try {
      const [placesData, weatherData] = await Promise.all([
        fetchNearbySafePlaces(latestLocation),
        fetchCurrentWeather(latestLocation),
      ]);
      setPlaces(placesData);
      setWeather(weatherData);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch safety guidance';
      setGuidanceError(msg);
    } finally {
      setLoadingGuidance(false);
    }
  }, [latestLocation]);

  useEffect(() => {
    if (latestLocation && places.length === 0 && !loadingGuidance) {
      loadGuidance();
    }
  }, [latestLocation, places.length, loadingGuidance, loadGuidance]);

  const handleOpenAdd = () => {
    setEditingContact(null);
    setFormName('');
    setFormPhone('');
    setIsEditorOpen(true);
  };

  const handleOpenEdit = (c: TrustedContact) => {
    setEditingContact(c);
    setFormName(c.name);
    setFormPhone(c.phone);
    setIsEditorOpen(true);
  };

  const handleSaveContact = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim() || !formPhone.trim()) return;

    setSaving(true);
    const res = await contactsService.addOrUpdate(formName, formPhone, editingContact?.id);
    setSaving(false);
    if (res.success) {
      setContacts(res.contacts);
      setIsEditorOpen(false);
    } else {
      onShowAlertNotification(res.error || 'Failed to save contact');
    }
  };

  const handleToggleVerified = async (id: string) => {
    const updated = await contactsService.toggleVerified(id);
    setContacts(updated);
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    const updated = await contactsService.remove(deleteConfirm.id);
    setContacts(updated);
    setDeleteConfirm(null);
  };

  const handleTestAlert = (contact: TrustedContact) => {
    if (onOpenSendMessageModal) {
      onOpenSendMessageModal(contact, 'test');
    } else {
      onShowAlertNotification(
        `TEST ALERT: SafeRescue simulated emergency ping sent to ${contact.name} (${contact.phone}).`
      );
    }
  };

  return (
    <div className="space-y-4 pb-20">
      <div>
        <h2 className="text-2xl font-extrabold text-white">Trusted Contacts</h2>
        <p className="text-xs text-[#8E9BB6] mt-0.5">
          Configure trusted contacts and send real emergency alerts or test pings via SMS or WhatsApp.
        </p>
      </div>

      <div className="rounded-[20px] p-4 bg-gradient-to-r from-[#20102B] to-[#121936] border border-[#5B4BDB]/40 text-white shadow-md flex items-center justify-between">
        <div>
          <h4 className="font-bold text-sm text-white flex items-center gap-1.5">
            <Send className="w-4 h-4 text-[#8B7CFF]" /> Send Real Emergency Message
          </h4>
          <p className="text-[11px] text-[#A5B4FC] mt-0.5">
            Dispatch live GPS, distress alert & victim info via your phone/laptop's messaging app.
          </p>
        </div>
        <button
          type="button"
          id="btn-open-real-message-modal"
          onClick={() => onOpenSendMessageModal?.(undefined, 'emergency')}
          className="px-3.5 py-2 rounded-xl bg-[#5B4BDB] hover:bg-[#4838c4] text-white text-xs font-bold shrink-0 transition-colors shadow-sm"
        >
          Send Now
        </button>
      </div>

      <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-[#222236]">{contacts.length}/3 Configured</h4>
              <p className="text-xs text-[#777788]">
                Contacts are saved to your secure account and sync across devices.
              </p>
            </div>
          </div>

          {contacts.length < 3 && (
            <button
              type="button"
              id="btn-add-contact"
              onClick={handleOpenAdd}
              className="px-3 py-1.5 rounded-xl bg-[#5B4BDB] hover:bg-[#4838c4] text-white text-xs font-bold flex items-center gap-1.5 transition-colors"
            >
              <UserPlus className="w-3.5 h-3.5" /> Add
            </button>
          )}
        </div>
      </div>

      <div className="space-y-2.5">
        {contacts.map((contact) => (
          <div
            key={contact.id}
            className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-sm flex flex-col gap-2.5"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-[#5B4BDB]/10 text-[#5B4BDB] flex items-center justify-center font-bold text-sm">
                  {contact.name[0]?.toUpperCase() || 'C'}
                </div>
                <div>
                  <h5 className="font-bold text-sm text-[#222236]">{contact.name}</h5>
                  <p className="text-xs text-[#555566] font-mono font-medium">{contact.phone}</p>
                </div>
              </div>

              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => onOpenSendMessageModal?.(contact, 'emergency')}
                  className="px-2.5 py-1 rounded-lg bg-[#E51E4D] hover:bg-[#c91841] text-white text-[11px] font-bold flex items-center gap-1 shadow-sm transition-colors"
                  title="Send Emergency Alert to this contact"
                >
                  <Send className="w-3 h-3" /> SOS SMS
                </button>
                <button
                  type="button"
                  onClick={() => onOpenSendMessageModal?.(contact, 'test')}
                  className="px-2.5 py-1 rounded-lg bg-[#16A34A] hover:bg-[#15803D] text-white text-[11px] font-bold flex items-center gap-1 shadow-sm transition-colors"
                  title="Send Test or WhatsApp message"
                >
                  <MessageSquare className="w-3 h-3" /> WhatsApp
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between pt-1 border-t border-zinc-100">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleToggleVerified(contact.id)}
                  className="px-2.5 py-1 rounded-lg border border-zinc-300 hover:bg-zinc-50 text-[11px] font-semibold text-[#222236] transition-colors"
                >
                  {contact.verified ? 'Verified ✓' : 'Mark Verified'}
                </button>
                <button
                  type="button"
                  onClick={() => handleTestAlert(contact)}
                  className="px-2.5 py-1 rounded-lg border border-[#5B4BDB] text-[#5B4BDB] hover:bg-[#5B4BDB]/10 text-[11px] font-semibold transition-colors"
                >
                  Test Alert Options
                </button>
              </div>

              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => handleOpenEdit(contact)}
                  className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-800 hover:bg-zinc-100"
                  title="Edit Contact Phone Number"
                >
                  <Edit2 className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => setDeleteConfirm(contact)}
                  className="p-1.5 rounded-lg text-rose-500 hover:text-rose-700 hover:bg-rose-50"
                  title="Remove Contact"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        ))}

        {contacts.length === 0 && (
          <div className="rounded-[18px] p-6 bg-[#0D2140] text-center text-white border border-white/5 space-y-2">
            <Users className="w-8 h-8 text-[#55D7FF] mx-auto opacity-70" />
            <h5 className="font-bold text-sm">No Trusted Contacts Yet</h5>
            <p className="text-xs text-[#B8C4D9]">
              Add up to 3 people you trust. Their info is saved securely to your account.
            </p>
          </div>
        )}
      </div>

      <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-md space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
              <Navigation className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-[#222236]">Nearby Safety Guidance</h4>
              <p className="text-xs text-[#777788]">Live mapped places + weather</p>
            </div>
          </div>

          <button
            type="button"
            id="btn-refresh-guidance"
            onClick={loadGuidance}
            disabled={!latestLocation || loadingGuidance}
            className="p-2 rounded-xl text-[#5B4BDB] hover:bg-[#F1EFFF] transition-colors disabled:opacity-40"
            title="Refresh Nearby Guidance"
          >
            <RefreshCw className={`w-4 h-4 ${loadingGuidance ? 'animate-spin' : ''}`} />
          </button>
        </div>

        <p className="text-[11px] text-[#777788] leading-relaxed">
          {latestLocation
            ? 'Uses your latest location fix. Coordinates sent securely via HTTPS to OpenStreetMap Overpass and Open-Meteo.'
            : 'No current GPS fix available. Start an SOS or grant browser location access to view live safe places.'}
        </p>

        {weather && (
          <div className="rounded-xl p-3 bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <Sun className="w-4 h-4 text-amber-500" />
              <span className="font-bold text-zinc-800">
                {weather.weatherDescription} • {weather.temperatureC}°C
              </span>
            </div>
            <span className="text-zinc-500">
              Wind: {weather.windSpeedKmh} km/h • Humidity: {weather.humidityPercent}%
            </span>
          </div>
        )}

        <InteractiveSafeMap
          location={latestLocation}
          safePlaces={places}
          unsafeZones={localAiEngine.evaluateUnsafeZones(latestLocation)}
          recommendedEscapeRoute={
            places.length > 0
              ? {
                  destinationName: places[0].name,
                  distanceMeters: places[0].distanceMeters || 350,
                  estimatedWalkMinutes: Math.max(1, Math.round((places[0].distanceMeters || 350) / 80)),
                  guidanceStep: `Direct corridor toward ${places[0].name} (${places[0].category.replace('_', ' ')}). Avoid adjacent unlit pathways.`,
                }
              : null
          }
        />

        {places.length > 0 && (
          <div className="space-y-1.5 pt-1">
            <span className="text-[11px] font-bold uppercase tracking-wider text-zinc-500">
              Nearby Emergency & Safe Facilities ({places.length})
            </span>
            {places.slice(0, 5).map((place) => (
              <div
                key={place.id}
                className="p-2.5 rounded-lg bg-zinc-50 border border-zinc-100 flex items-center justify-between text-xs"
              >
                <div className="flex items-center gap-2 truncate">
                  <span
                    className={`w-2 h-2 rounded-full shrink-0 ${
                      place.category === 'police'
                        ? 'bg-blue-600'
                        : place.category === 'hospital'
                          ? 'bg-red-600'
                          : place.category === 'fire_station'
                            ? 'bg-amber-600'
                            : 'bg-emerald-600'
                    }`}
                  />
                  <div className="truncate">
                    <span className="font-semibold text-zinc-900 block truncate">{place.name}</span>
                    {place.address && (
                      <span className="text-[10px] text-zinc-500 block truncate">
                        {place.address}
                      </span>
                    )}
                  </div>
                </div>
                {place.distanceMeters && (
                  <span className="text-[11px] font-bold text-[#5B4BDB] shrink-0 ml-2">
                    {place.distanceMeters > 1000
                      ? `${(place.distanceMeters / 1000).toFixed(1)} km`
                      : `${place.distanceMeters} m`}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}

        {guidanceError && <p className="text-xs text-rose-600">{guidanceError}</p>}
      </div>

      <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-md space-y-2">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-[#222236]">SafeRescue Alerts</h4>
            <p className="text-xs text-[#777788]">Browser notifications & lock-screen access</p>
          </div>
        </div>
        <p className="text-[11px] text-[#777788] leading-relaxed">
          Emergency notifications keep access buttons readily available in case of distress.
        </p>
      </div>

      <div className="space-y-2 pt-2">
        <h4 className="text-sm font-bold text-white">Emergency Subsystem Status</h4>
        {[
          { title: 'Emergency Access & Countdown', status: 'Active' },
          { title: 'Safe Places & Live Weather', status: 'Active' },
          { title: 'Real Database Sync', status: 'Active' },
          { title: 'Trusted Contacts Cloud Storage', status: 'Active' },
        ].map((item, idx) => (
          <div
            key={idx}
            className="rounded-[16px] p-3.5 bg-white text-[#17172A] flex items-center justify-between shadow-sm"
          >
            <div className="flex items-center gap-2.5">
              <Shield className="w-4 h-4 text-[#5B4BDB]" />
              <span className="text-xs font-bold text-[#222236]">{item.title}</span>
            </div>
            <span className="text-[11px] text-[#777788] font-medium">{item.status}</span>
          </div>
        ))}
      </div>

      {isEditorOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 flex items-center justify-center p-4">
          <div className="w-full max-w-sm rounded-[24px] bg-white text-[#17172A] p-5 shadow-2xl space-y-4">
            <h3 className="font-bold text-base">
              {editingContact ? 'Edit Trusted Contact' : 'Add Trusted Contact'}
            </h3>
            <form onSubmit={handleSaveContact} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-zinc-600 block mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. John Doe"
                  className="w-full px-3 py-2 rounded-xl border border-zinc-300 text-sm focus:outline-none focus:ring-2 focus:ring-[#5B4BDB]"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-600 block mb-1">
                  Phone Number
                </label>
                <input
                  type="tel"
                  required
                  value={formPhone}
                  onChange={(e) => setFormPhone(e.target.value)}
                  placeholder="e.g. +1 555-0142"
                  className="w-full px-3 py-2 rounded-xl border border-zinc-300 text-sm focus:outline-none focus:ring-2 focus:ring-[#5B4BDB]"
                />
              </div>

              <p className="text-[11px] text-zinc-500">
                Saving changes resets the verification flag.
              </p>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsEditorOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-zinc-600 hover:bg-zinc-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 rounded-xl text-xs font-bold bg-[#5B4BDB] text-white hover:bg-[#4838c4] disabled:opacity-60"
                >
                  {saving ? 'Saving...' : 'Save Contact'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteConfirm && (
        <div className="fixed inset-0 z-50 bg-black/75 flex items-center justify-center p-4">
          <div className="w-full max-w-sm rounded-[24px] bg-white text-[#17172A] p-5 shadow-2xl space-y-3">
            <h3 className="font-bold text-base">Remove Trusted Contact?</h3>
            <p className="text-xs text-zinc-600 leading-relaxed">
              Are you sure you want to remove <span className="font-bold">{deleteConfirm.name}</span>{' '}
              from your trusted contact list?
            </p>
            <div className="flex items-center justify-end gap-2 pt-3">
              <button
                type="button"
                onClick={() => setDeleteConfirm(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-zinc-600 hover:bg-zinc-100"
              >
                Keep
              </button>
              <button
                type="button"
                onClick={handleDelete}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-rose-600 text-white hover:bg-rose-700"
              >
                Remove
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
