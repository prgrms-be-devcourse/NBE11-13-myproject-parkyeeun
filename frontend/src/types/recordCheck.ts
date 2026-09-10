export type RecordCheckItem = {
  date: string;
  tilExists: boolean;
  readmeEntryExists: boolean;
};

export type RecordCheckResponse = {
  month: string;
  items: RecordCheckItem[];
};
