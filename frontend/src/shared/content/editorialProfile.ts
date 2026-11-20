/**
 * The single public editorial profile of Ajarline.
 *
 * It is static application content, not a field of Article data.
 * Changing it needs a code change and a new deployment.
 * Every field must have text. A missing field is a compile error.
 * An empty one is caught by a test.
 */
export const EDITORIAL_PROFILE = {
  displayName: 'Nguyễn Minh Toàn',
  responsibilityLabel: 'Người chịu trách nhiệm nội dung',
  // NOTE: draft text. The owner writes the real short bio.
  shortBio: 'Một người làm phần mềm, tự viết và biên tập mọi nội dung trên Ajarline.',
} as const
