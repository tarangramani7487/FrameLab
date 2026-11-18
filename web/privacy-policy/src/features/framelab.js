// FrameLab features & functionality (checked against the Android app and design/HANDOFF.md).
// Only list what the app really does. Unfinished items go in `coming`, never in the main list.
export default {
  kicker: 'FEATURES',
  title: 'Seamless carousels, made from your photos.',
  intro:
    'FrameLab turns your photos into Instagram carousel posts. Pick a template, fill the photo spots, ' +
    'and save every slide as a full-size image, numbered and in order. Photos and frames cross from one ' +
    'slide into the next, so the post reads as one long picture when you swipe.',

  // Landing hero.
  hero: {
    kicker: 'CAROUSEL MAKER FOR ANDROID',
    title: ['One photo.', 'Three slides.', 'One seamless swipe.'],
    text: 'FrameLab turns your photos into Instagram carousels that read as one long picture. Pick a template, drop in your photos, and save every slide at full size, in order.',
  },

  // Facts only (see sections below).
  stats: [
    { value: '120+', label: 'carousel templates' },
    { value: '4', label: 'sizes: 1:1, 4:5, 3:4, 9:16' },
    { value: '1–6+', label: 'slides per post' },
    { value: '0', label: 'photos uploaded' },
  ],

  steps: [
    { title: 'Pick a design', text: 'Choose a multi-slide template or a single frame from Home or Explore.', shot: 'main' },
    { title: 'Add your photos', text: 'Pick photos once and they fill the spots in order, slide by slide.', shot: 'e4-filled' },
    { title: 'Adjust', text: 'Move, resize, rotate, crop and restyle each photo. Edit the text and add stickers.', shot: 'e7-selected' },
    { title: 'Save and post', text: 'Every slide is saved to your gallery at full size, numbered so you post them in the right order.', shot: 'n9-postinorder' },
  ],

  sections: [
    {
      id: 'templates',
      icon: 'templates',
      title: 'Templates and frames',
      items: [
        { label: 'Template library', text: 'More than 120 carousel templates in rows like Instagram Trending, Torn Paper Stories, Travel, Film, Grids, Food, Pets and Wedding. New templates arrive from our servers without an app update.' },
        { label: 'Frames', text: 'Single decorative frames (polaroids, lab prints, film cameras, stamps) for one photo.' },
        { label: 'Every size', text: 'Square 1:1, portrait 4:5, 3:4 and stories 9:16, from 1 to 6+ slides.' },
        { label: 'Live previews', text: 'Swipe through every slide, and see all slides joined to check how the swipe flows, before you start.' },
      ],
    },
    {
      id: 'find',
      icon: 'search',
      title: 'Find the right design',
      items: [
        { label: 'Search', text: 'Search by name, category, style or topic, with live results and suggestions.' },
        { label: 'Explore filters', text: 'Filter by number of slides, number of photos, ratio, style (torn paper, polaroid, film, stamp, minimal) and topic (travel, love, birthday, food, pets, fitness).' },
        { label: 'Sort', text: 'Trending, New and Most used.' },
        { label: 'Favourites', text: 'Tap the heart on any template to keep it in My Designs → Favourites.' },
      ],
    },
    {
      id: 'editor',
      icon: 'position',
      title: 'The editor',
      items: [
        { label: 'Seamless spots', text: 'A photo can span two slides. FrameLab keeps it lined up across the cut, so the swipe looks continuous.' },
        { label: 'Fill in order', text: 'Pick several photos at once and they fill the empty spots in the template’s order. Shuffle them, or change the order in the Order panel.' },
        { label: 'Move and resize', text: 'Drag to move, corners to resize, sides to stretch, and rotate with optional 90° snapping.' },
        { label: 'Adjust inside', text: 'Crop and zoom the picture inside its frame with a rule-of-thirds grid. Turn, centre, fill, fit or mirror the photo.' },
        { label: 'Frame shapes', text: 'Original, square, rounded, circle and arch.' },
        { label: 'Lock', text: 'Lock a photo so it can’t be moved or deleted by accident.' },
        { label: 'Text and stickers', text: 'Edit any template text and its font, and add stickers: tape, stamps, dates, doodles and chips.' },
        { label: 'Background colour', text: 'Change the colour behind the paper and photos, or go back to the template’s own colour.' },
        { label: 'Slides', text: 'Add slides and see which slides are linked by a shared photo.' },
        { label: 'Undo and redo', text: 'Step back and forward through your changes, and preview the post full screen before saving.' },
      ],
    },
    {
      id: 'export',
      icon: 'download',
      title: 'Saving and sharing',
      items: [
        { label: 'Full-size images', text: 'Each slide is saved as its own image (for example 1080 × 1350 for 4:5), in High or Max quality, as JPG or PNG.' },
        { label: 'In order', text: 'Files are numbered (torn-diary-01.jpg, -02…) and can go into a “FrameLab” album in your gallery.' },
        { label: 'Empty-spot check', text: 'Before saving, FrameLab tells you which slides still have empty spots.' },
        { label: 'Post in this order', text: 'A step-by-step guide hands the images to Instagram in the right order.' },
        { label: 'Saved posts', text: 'Every saved post stays in My Designs, where you can share it again, save it again or edit a copy.' },
      ],
    },
    {
      id: 'library',
      icon: 'designs',
      title: 'My Designs',
      items: [
        { label: 'Drafts', text: 'Leave the editor with “Save draft” and pick up where you left off. Each draft shows how many photos are placed.' },
        { label: 'Manage', text: 'Rename, duplicate, select several or delete drafts. Your original photos always stay in your gallery.' },
        { label: 'Continue last draft', text: 'The Create button offers your most recent draft first.' },
      ],
    },
    {
      id: 'reliability',
      icon: 'offline',
      title: 'Works anywhere',
      items: [
        { label: 'Offline', text: 'The last downloaded templates and a set of built-in designs keep working without a connection.' },
        { label: 'On your device', text: 'Photos are picked with the Android photo picker and processed on your phone. They are not uploaded.' },
        { label: 'Comfortable to use', text: 'Light, dark or system theme for browsing, an always-dark photo-first editor, large touch targets and screen-reader labels.' },
      ],
    },
  ],

  pro: {
    title: 'FrameLab Pro',
    text: 'Pro unlocks the Pro templates marked in the library. Prices and plans are shown in the app before you subscribe.',
  },

  // Built in the app but not live yet. Keep this honest; remove items as they ship.
  coming: [
    'Sign in with Google, Apple or email to keep drafts and favourites on all your devices.',
    'More frames served from our own library.',
  ],
}
