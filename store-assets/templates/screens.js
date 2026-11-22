// Pixel AI store graphics: copy, layout and image names.
// Drop real outputs into store-assets/images/<name>.jpg (or .png). Missing images show a labelled placeholder.
// Headline rules (Google Play): no "best", "#1", "top", "new", "trending", "free", prices, rankings or "download now".

const APP_NAME = "Pixel AI"; // TODO: final relaunch name

const IMG = (name) => `../images/${name}.jpg`;

function slot(name, label) {
  return `<div class="slot"><div class="ph"><b>${label}</b><code>${name}.jpg</code></div>` +
    `<img src="${IMG(name)}" alt="" onerror="imgFallback(this)"></div>`;
}
window.imgFallback = (img) => {
  if (img.src.endsWith(".jpg")) img.src = img.src.replace(/\.jpg$/, ".png");
  else img.remove();
};
const px = (o) => Object.entries(o).map(([k, v]) => `${k}:${typeof v === "number" ? v + "px" : v}`).join(";");

// playY moves the play button down (percent of card height) when it would cover a face
function vid({ name, label, tag, box, time = "00:05", p = "38%", sm = false, tagPos = "", rot = 0, playY = 50 }) {
  return `<div class="vid${sm ? " sm" : ""}" style="${px(box)};transform:rotate(${rot}deg)">${slot(name, label)}<div class="shade"></div>` +
    `<div class="play" style="top:${playY}%"></div>` +
    (tag ? `<div class="tag ${tagPos}">${tag}</div>` : "") +
    `<div class="scrub" style="--p:${p}"><span>${time}</span><i></i><span>00:12</span></div></div>`;
}
function tile({ name, label, tag, box, play = true }) {
  return `<div class="tile" style="${px(box)}">${slot(name, label)}` +
    (play ? `<div class="vid sm" style="position:absolute;inset:0;box-shadow:none;border-radius:0;background:none"><div class="play" style="transform:scale(.7)"></div></div>` : "") +
    (tag ? `<div class="tag sm">${tag}</div>` : "") + `</div>`;
}
function pol({ name, label, box, r = 0, cap = "", old = false }) {
  return `<div class="pol${old ? " old" : ""}" style="${px(box)};--r:${r}deg"><div class="in">${slot(name, label)}</div>` +
    (cap ? `<div class="cap">${cap}</div>` : "") + `</div>`;
}
// curved arrow from (x1,y1) to (x2,y2), bending by `bend`
function arrow(x1, y1, x2, y2, bend = 80, id = Math.random().toString(36).slice(2)) {
  const mx = (x1 + x2) / 2 + bend, my = (y1 + y2) / 2 - Math.abs(bend) * 0.4;
  const ang = Math.atan2(y2 - my, x2 - mx), L = 34;
  const a1 = [x2 - L * Math.cos(ang - 0.5), y2 - L * Math.sin(ang - 0.5)];
  const a2 = [x2 - L * Math.cos(ang + 0.5), y2 - L * Math.sin(ang + 0.5)];
  return `<svg class="arrow" width="1080" height="1920" style="left:0;top:0;pointer-events:none" viewBox="0 0 1080 1920">
    <defs><linearGradient id="g${id}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#FF9A3D"/><stop offset="1" stop-color="#FF3D7F"/></linearGradient></defs>
    <path d="M${x1} ${y1} Q${mx} ${my} ${x2} ${y2}" fill="none" stroke="url(#g${id})" stroke-width="9" stroke-linecap="round" stroke-dasharray="2 20"/>
    <path d="M${a1[0]} ${a1[1]} L${x2} ${y2} L${a2[0]} ${a2[1]}" fill="none" stroke="#FF3D7F" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`;
}
function head(l1, l2, sub) {
  return `<header class="head"><h1>${l1}<br><span class="g">${l2}</span></h1>${sub ? `<p>${sub}</p>` : ""}</header>`;
}
function chips(list, top) {
  return `<div class="chips" style="top:${top}px">${list.map((c, i) => `<span class="chip${i === 0 ? " on" : ""}">${c}</span>`).join("")}</div>`;
}

// ---------------------------------------------------------------- screenshots (1080x1920)
const SCREENS = [
  {
    id: "ss1", title: "01 · AI Kiss", glow: "#FF2E7E", glow2: "#9B2CFF",
    html: () =>
      head("2 Photos.", "1 AI Kiss.", "Turn two selfies into a kiss video") +
      vid({ name: "kiss", label: "Kiss video", tag: "AI Kiss Video", box: { left: 150, top: 560, width: 780, height: 1060 }, time: "00:04" }) +
      pol({ name: "kiss_in1", label: "Photo 1", box: { left: 46, top: 1360, width: 250, height: 320 }, r: -9 }) +
      pol({ name: "kiss_in2", label: "Photo 2", box: { left: 786, top: 1330, width: 250, height: 320 }, r: 8 }) +
      arrow(250, 1350, 300, 1180, -60) + arrow(840, 1320, 790, 1150, 60) +
      chips(["Kiss", "Blow Kiss"], 1736),
  },
  {
    id: "ss2", title: "02 · AI Hug", glow: "#FFB020", glow2: "#FF4D3D",
    html: () =>
      head("Hug Someone", "You Miss", "Works with old family photos too") +
      vid({ name: "hug", label: "Hug video", tag: "AI Hug Video", box: { left: 150, top: 560, width: 780, height: 1060 }, time: "00:06", p: "52%", playY: 70 }) +
      // grandson is on the left of hug.jpg and grandma on the right, so the inputs follow that order
      pol({ name: "hug_in2", label: "Today", box: { left: 40, top: 1400, width: 270, height: 340 }, r: -8, cap: "2026" }) +
      pol({ name: "hug_in1", label: "Old photo", box: { left: 770, top: 1380, width: 270, height: 340 }, r: 7, cap: "1998", old: true }) +
      arrow(260, 1390, 310, 1200, -60) + arrow(830, 1370, 780, 1180, 60),
  },
  {
    // Trust screenshot. Backed by the backend: video credits are debited only in completeVideoEffectJob (success),
    // VIDEO_EFFECT_COST = 200. Photos are never used for training; kept until the user asks or deletes the profile.
    id: "ss3", title: "03 · No charge on failure", glow: "#22C58B", glow2: "#3D7BFF",
    html: () =>
      head("Video Failed?", "No Charge.", "Credits are only used when your video is ready") +
      `<div class="panel" style="${px({ left: 90, top: 600, width: 900, height: 560 })}">
        <div class="panel-h">My videos</div>
        <div class="row">
          <div class="thumb">${slot("kiss", "Kiss")}</div>
          <div class="row-t"><b>AI Kiss Video</b><span>Ready to save</span></div>
          <div class="status ok">−200 credits</div>
        </div>
        <div class="row">
          <div class="thumb dim">${slot("hug", "Hug")}</div>
          <div class="row-t"><b>AI Hug Video</b><span>Didn't finish · Try again</span></div>
          <div class="status none">0 credits</div>
        </div>
      </div>` +
      `<div class="perks" style="${px({ left: 90, top: 1290, width: 900 })}">
        <div class="perk"><i>✓</i><span>Pay only for videos that finish</span></div>
        <div class="perk"><i>✓</i><span>Photos never used to train AI</span></div>
        <div class="perk"><i>✓</i><span>Delete your photos anytime</span></div>
      </div>`,
  },
  {
    id: "ss4", title: "04 · Dance", glow: "#9D3BFF", glow2: "#FF2EC4",
    html: () =>
      head("Any Photo", "Can Dance", "7 dance effects from one photo") +
      // tall 2:3 card so the full-body dance pose is not cropped
      vid({ name: "dance", label: "Phut Hon Dance", tag: "Phut Hon Dance", box: { left: 70, top: 540, width: 600, height: 890 }, time: "00:03", p: "30%", playY: 62 }) +
      pol({ name: "dance_in", label: "Your photo", box: { left: 730, top: 640, width: 290, height: 420 }, r: 6, cap: "Your photo" }) +
      arrow(800, 1090, 690, 1250, -50) +
      // store-safe labels; the in-app effect names (Twerk Dance, Hip Shake, Jiggle Dance, Squat Shake) stay in the app only
      tile({ name: "twerk", label: "Twerk", tag: "Hip-Hop", box: { left: 90, top: 1474, width: 210, height: 360 } }) +
      tile({ name: "hip_shake", label: "Hip Shake", tag: "Latin Groove", box: { left: 320, top: 1474, width: 210, height: 360 } }) +
      tile({ name: "jiggle", label: "Jiggle", tag: "Party Bounce", box: { left: 550, top: 1474, width: 210, height: 360 } }) +
      tile({ name: "squat_shake", label: "Squat", tag: "Shake It", box: { left: 780, top: 1474, width: 210, height: 360 } }),
  },
  {
    id: "ss5", title: "05 · Muscle Show", glow: "#FF4A1F", glow2: "#FFB23D",
    html: () =>
      head("Flex Like a", "Superhero", "Muscle Show turns any photo into a flex") +
      // wide card so both flexed arms stay in frame
      vid({ name: "muscle", label: "Muscle Show", tag: "Muscle Show", box: { left: 80, top: 560, width: 920, height: 1010 }, time: "00:04", p: "60%", playY: 58 }) +
      pol({ name: "muscle_in", label: "Before", box: { left: 46, top: 1440, width: 290, height: 390 }, r: -8, cap: "Before" }) +
      arrow(330, 1460, 420, 1330, -60),
  },
  {
    id: "ss6", title: "06 · Fire & Thunder", glow: "#FF6A00", glow2: "#2F7BFF",
    html: () =>
      head("Breathe Fire.", "Call Thunder.", "Spitfire, Set on Fire, Thunder God") +
      vid({ name: "spitfire", label: "Spitfire", tag: "Spitfire", box: { left: 90, top: 560, width: 900, height: 820 }, time: "00:02", p: "22%" }) +
      tile({ name: "set_on_fire", label: "Set on Fire", tag: "Set on Fire", box: { left: 90, top: 1440, width: 435, height: 400 } }) +
      tile({ name: "thunder_god", label: "Thunder God", tag: "Thunder God", box: { left: 555, top: 1440, width: 435, height: 400 } }) +
      // top-left: the fire stream fills the card's right side
      pol({ name: "power_in", label: "Your photo", box: { left: 36, top: 490, width: 210, height: 280 }, r: -8 }),
  },
  {
    id: "ss7", title: "07 · Magic", glow: "#00C2B8", glow2: "#3D6BFF",
    html: () =>
      head("Become a", "Mermaid.", "Plus alien abduction and liquid metal") +
      tile({ name: "mermaid", label: "Mermaid", tag: "Mermaid Spell", box: { left: 54, top: 640, width: 310, height: 860 } }) +
      tile({ name: "alien", label: "Alien Kidnap", tag: "Alien Kidnap", box: { left: 385, top: 580, width: 310, height: 920 } }) +
      tile({ name: "metal", label: "Turning Metal", tag: "Turning Metal", box: { left: 716, top: 640, width: 310, height: 860 } }) +
      pol({ name: "magic_in", label: "Your photo", box: { left: 420, top: 1560, width: 240, height: 300 }, r: -3 }) +
      arrow(410, 1640, 220, 1520, 40) + arrow(540, 1550, 540, 1520, 0) + arrow(670, 1640, 860, 1520, -40),
  },
  {
    id: "ss8", title: "08 · Anime to Real", glow: "#FF3DB0", glow2: "#3D7BFF",
    html: () =>
      head("Anime to", "Real Life", "Turn your anime art into a real photo") +
      `<div class="vid split" style="${px({ left: 90, top: 560, width: 900, height: 1240 })}">
        <div class="half" style="left:0;width:450px"><div class="abs" style="left:0;top:0;bottom:0;width:900px">${slot("anime", "Anime")}</div></div>
        <div class="half" style="left:450px;width:450px"><div class="abs" style="left:-450px;top:0;bottom:0;width:900px">${slot("real", "Real")}</div></div>
        <div class="knob"></div>
        <div class="tag top" style="left:200px">Anime</div>
        <div class="tag top" style="left:680px">Real</div>
      </div>`,
  },
  {
    // spare: replaced by the trust screenshot in the store set; export with ./export.sh art
    id: "art", title: "Spare · AI Art", glow: "#7A4DFF", glow2: "#FF3D7F",
    html: () =>
      head("Type It.", "See It.", "AI art in Realistic, Anime, 3D and more") +
      `<div class="prompt" style="${px({ left: 70, top: 560, width: 940, height: 112 })}"><span>a girl in a neon city at night, rain</span><b>Create</b></div>` +
      tile({ name: "art_realistic", label: "Realistic", tag: "Realistic", box: { left: 90, top: 740, width: 435, height: 520 }, play: false }) +
      tile({ name: "art_anime", label: "Anime", tag: "Anime", box: { left: 555, top: 740, width: 435, height: 520 }, play: false }) +
      tile({ name: "art_3d", label: "3D Art", tag: "3D Art", box: { left: 90, top: 1290, width: 435, height: 520 }, play: false }) +
      tile({ name: "art_fiction", label: "Fiction", tag: "Fiction", box: { left: 555, top: 1290, width: 435, height: 520 }, play: false }),
  },
];

// ---------------------------------------------------------------- icon (512x512) and feature graphic (1024x500)
const iconHTML = () => `<div class="frame icon" id="icon-frame">${slot("icon", "Icon photo")}<div class="wash"></div><div class="ai">AI</div></div>`;

const EXTRAS = [
  { id: "icon", title: "Icon · 512×512", w: 512, h: 512, cls: "icon", raw: iconHTML },
  {
    id: "feature", title: "Feature graphic · 1024×500", w: 1024, h: 500, cls: "fg",
    html: () =>
      `<div class="bg"></div>
      <div class="brand"><div class="mini">${iconHTML()}</div><b>${APP_NAME}</b></div>
      <h2>AI Kiss, Hug &amp; <span class="g">Dance Videos</span></h2>` +
      // side-card labels sit on the half the centre card doesn't cover
      vid({ name: "fg_hug", label: "Hug", tag: "Hug", tagPos: "l", box: { left: 545, top: 86, width: 190, height: 340 }, rot: -7 }) +
      vid({ name: "fg_dance", label: "Dance", tag: "Dance", tagPos: "r", box: { left: 845, top: 86, width: 165, height: 340 }, rot: 7 }) +
      vid({ name: "fg_kiss", label: "Kiss", tag: "Kiss", box: { left: 705, top: 54, width: 180, height: 390 } }),
  },
];

// ---------------------------------------------------------------- render
function frameHTML(a) {
  if (a.raw) return a.raw();
  const kind = a.cls || "ss";
  return `<div class="frame ${kind}" id="${a.id}-frame" style="--glow:${a.glow || "#FF3D7F"};--glow2:${a.glow2 || "#7A2BFF"}">` +
    (kind === "ss" ? `<div class="bg"></div>` : "") + a.html() + `</div>`;
}

const only = new URLSearchParams(location.search).get("only");
const app = document.getElementById("app");
const all = [...SCREENS, ...EXTRAS];

if (only) {
  document.body.classList.add("only");
  const a = all.find((x) => x.id === only);
  app.innerHTML = a ? frameHTML(a) : `<p>Unknown asset "${only}"</p>`;
} else {
  const cell = (a, w, h, scale, round = false) =>
    `<div class="cell"><div class="shrink${round ? " round" : ""}" style="width:${w * scale}px;height:${h * scale}px">` +
    frameHTML(a).replace(/^<div class="frame([^"]*)"/, `<div class="frame$1" data-scale="${scale}"`) +
    `</div><a href="?only=${a.id}">${a.title} ↗</a></div>`;
  app.innerHTML = `<div class="board">
    <div class="board-head"><h1>${APP_NAME} store graphics</h1>
      <p>8 phone screenshots (1080×1920), icon (512×512) and feature graphic (1024×500). Striped boxes are placeholders: save a real app output as
      <code>store-assets/images/&lt;name&gt;.jpg</code> and it replaces the box. Export with <code>store-assets/export.sh</code>.</p></div>
    <div class="row">${SCREENS.map((s) => cell(s, 1080, 1920, 0.25)).join("")}</div>
    <div class="row">${cell(EXTRAS[0], 512, 512, 0.5)}${cell(EXTRAS[0], 512, 512, 0.5, true).replace("Icon · 512×512", "Icon · as Play shows it")}${cell(EXTRAS[1], 1024, 500, 0.5)}</div>
  </div>`;
  document.querySelectorAll("[data-scale]").forEach((f) => (f.style.transform = `scale(${f.dataset.scale})`));
}
