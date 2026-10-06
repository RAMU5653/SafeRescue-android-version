import { TimelineEvent, TimelineEventType } from '../types';
import { supabase } from '../lib/supabase';

interface DbTimelineEvent {
  id: string;
  user_id: string;
  event_type: string;
  title: string;
  detail: string;
  risk_score: number | null;
  timestamp_millis: number;
  created_at: string;
}

function toTimelineEvent(row: DbTimelineEvent): TimelineEvent {
  return {
    id: row.id,
    timestampMillis: row.timestamp_millis,
    type: row.event_type as TimelineEventType,
    title: row.title,
    detail: row.detail,
    riskScore: row.risk_score,
  };
}

class TimelineService {
  async loadTimeline(): Promise<TimelineEvent[]> {
    const { data, error } = await supabase
      .from('timeline_events')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(100);

    if (error) {
      console.warn('Failed to load timeline:', error.message);
      return [];
    }

    return (data as DbTimelineEvent[]).map(toTimelineEvent);
  }

  async addEvent(
    type: TimelineEventType,
    title: string,
    detail: string,
    score?: number
  ): Promise<TimelineEvent | null> {
    const { data, error } = await supabase
      .from('timeline_events')
      .insert({
        event_type: type,
        title,
        detail,
        risk_score: score ?? null,
        timestamp_millis: Date.now(),
      })
      .select('*')
      .maybeSingle();

    if (error) {
      console.warn('Failed to save timeline event:', error.message);
      return null;
    }

    if (!data) return null;
    return toTimelineEvent(data as DbTimelineEvent);
  }

  async clearTimeline(): Promise<void> {
    const { error } = await supabase
      .from('timeline_events')
      .delete()
      .neq('id', '00000000-0000-0000-0000-000000000000');

    if (error) {
      console.warn('Failed to clear timeline:', error.message);
    }
  }
}

export const timelineService = new TimelineService();
