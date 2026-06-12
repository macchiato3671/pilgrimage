const STORAGE_KEY = 'planDraft';

const createEmptyDraft = () => ({
  title: '',
  beginDate: '',
  endDate: '',
  details: [],
});

const parseJson = (value) => {
  try {
    return JSON.parse(value);
  } catch (error) {
    return null;
  }
};

export const getPlanDraft = () => {
  const savedDraft = sessionStorage.getItem(STORAGE_KEY);

  if (!savedDraft) return null;

  const draft = parseJson(savedDraft);

  if (!draft) {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }

  return {
    title: draft.title ?? '',
    beginDate: draft.beginDate ?? '',
    endDate: draft.endDate ?? '',
    details: Array.isArray(draft.details) ? draft.details : [],
  };
};

export const savePlanDraft = (draft) => {
  const nextDraft = {
    title: draft.title ?? '',
    beginDate: draft.beginDate ?? '',
    endDate: draft.endDate ?? '',
    details: Array.isArray(draft.details) ? draft.details : [],
  };

  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(nextDraft));

  return nextDraft;
};

export const setPlanBaseInfo = ({ title, beginDate, endDate }) => {
  const prevDraft = getPlanDraft() ?? createEmptyDraft();

  return savePlanDraft({
    ...prevDraft,
    title,
    beginDate,
    endDate,
  });
};

export const clearPlanDraft = () => {
  sessionStorage.removeItem(STORAGE_KEY);
};