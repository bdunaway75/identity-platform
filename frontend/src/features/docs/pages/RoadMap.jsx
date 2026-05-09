import "../styles/RoadMap.css";
import { useEffect, useMemo, useState } from "react";

const ROADMAP_API_URL =
  "https://api.github.com/repos/bdunaway75/identity-platform/issues?state=open&labels=enhancement&per_page=100&sort=updated&direction=desc";

function formatUpdatedAt(value) {
  if (!value) {
    return "";
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return "";
  }

  return parsedDate.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function summarizeBody(body) {
  const normalizedBody = String(body ?? "")
    .replace(/\r/g, "")
    .replace(/```[\s\S]*?```/g, "")
    .replace(/^#+\s+/gm, "")
    .replace(/!\[[^\]]*]\([^)]*\)/g, "")
    .replace(/\[([^\]]+)]\([^)]*\)/g, "$1")
    .trim();

  if (!normalizedBody) {
    return "Tracked in GitHub as an upcoming enhancement for the platform.";
  }

  const [firstBlock] = normalizedBody.split(/\n\s*\n/);
  const collapsed = firstBlock.replace(/\s+/g, " ").trim();

  if (collapsed.length <= 220) {
    return collapsed;
  }

  return `${collapsed.slice(0, 217).trimEnd()}...`;
}

export default function RoadMap() {
  const [issues, setIssues] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadRoadMap() {
      setStatus("loading");
      setError("");

      try {
        const response = await fetch(ROADMAP_API_URL, {
          headers: {
            Accept: "application/vnd.github+json",
          },
        });

        if (!response.ok) {
          throw new Error("Unable to load upcoming enhancements from GitHub.");
        }

        const data = await response.json();
        const issuesOnly = data.filter((item) => !item.pull_request);

        if (!isMounted) {
          return;
        }

        setIssues(issuesOnly);
        setStatus("ready");
      } catch (loadError) {
        console.error("Road map lookup failed", loadError);

        if (!isMounted) {
          return;
        }

        setIssues([]);
        setError(loadError.message || "Unable to load upcoming enhancements right now.");
        setStatus("error");
      }
    }

    loadRoadMap();

    return () => {
      isMounted = false;
    };
  }, []);

  const renderedIssues = useMemo(
    () =>
      issues.map((issue) => ({
        id: issue.id,
        number: issue.number,
        title: issue.title,
        url: issue.html_url,
        comments: issue.comments ?? 0,
        labels: Array.isArray(issue.labels)
          ? issue.labels.map((label) => ({
              id: label.id ?? label.name,
              name: label.name,
              color: label.color ? `#${label.color}` : "#6e7681",
            }))
          : [],
        updatedAt: formatUpdatedAt(issue.updated_at),
        summary: summarizeBody(issue.body),
      })),
    [issues]
  );

  return (
    <div className="roadmap-page">
      <section className="roadmap-hero">
        <div className="roadmap-repo-line">
          <span className="roadmap-repo-owner">bdunaway75</span>
          <span className="roadmap-repo-separator">/</span>
          <span className="roadmap-repo-name">identity-platform</span>
        </div>
        <div className="roadmap-hero-main">
          <div>
            <div className="docs-kicker">Road Map</div>
            <h1>The roads being paved to better support security and feasibility.</h1>
            <p>
              Open GitHub issues that represent upcoming Identity Platform work.
            </p>
          </div>
          <a
            href="https://github.com/bdunaway75/identity-platform/issues?q=is%3Aissue%20state%3Aopen%20label%3Aenhancement"
            target="_blank"
            rel="noreferrer"
            className="roadmap-github-link"
          >
            View issues
          </a>
        </div>
      </section>

      {status === "loading" ? (
        <section className="roadmap-state-card">
          <div className="roadmap-state-title">Loading planned work...</div>
          <div className="roadmap-state-copy">
            Pulling the latest enhancement issues from GitHub.
          </div>
        </section>
      ) : null}

      {status === "error" ? (
        <section className="roadmap-state-card roadmap-state-card-error">
          <div className="roadmap-state-title">Unable to load the road map</div>
          <div className="roadmap-state-copy">{error}</div>
        </section>
      ) : null}

      {status === "ready" ? (
        renderedIssues.length > 0 ? (
          <section className="roadmap-issue-list" aria-label="Upcoming enhancements">
            <div className="roadmap-list-toolbar">
              <div className="roadmap-open-count">
                <span className="roadmap-open-dot" aria-hidden="true" />
                {renderedIssues.length} open enhancements
              </div>
              <div className="roadmap-list-filter">Sort: Recently updated</div>
            </div>
            {renderedIssues.map((issue) => (
              <article className="roadmap-issue-row" key={issue.id}>
                <div className="roadmap-issue-icon" aria-hidden="true">
                  <svg viewBox="0 0 16 16" focusable="false">
                    <path d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13Zm0 1.25a5.25 5.25 0 1 1 0 10.5 5.25 5.25 0 0 1 0-10.5Z" />
                    <path d="M8 5.25a.75.75 0 0 1 .75.75v2.25a.75.75 0 0 1-1.5 0V6A.75.75 0 0 1 8 5.25Zm0 5.5a.8.8 0 1 0 0-1.6.8.8 0 0 0 0 1.6Z" />
                  </svg>
                </div>
                <div className="roadmap-issue-content">
                  <div className="roadmap-issue-title-line">
                    <a href={issue.url} target="_blank" rel="noreferrer" className="roadmap-issue-title">
                      {issue.title}
                    </a>
                    <div className="roadmap-labels" aria-label="Issue labels">
                      {issue.labels.map((label) => (
                        <span
                          className="roadmap-label"
                          key={label.id}
                          style={{ "--roadmap-label-color": label.color }}
                        >
                          {label.name}
                        </span>
                      ))}
                    </div>
                  </div>
                  <p>{issue.summary}</p>
                  <div className="roadmap-issue-meta">
                    #{issue.number} opened in bdunaway75/identity-platform · Updated {issue.updatedAt}
                    {issue.comments > 0 ? ` · ${issue.comments} comments` : ""}
                  </div>
                </div>
              </article>
            ))}
          </section>
        ) : (
          <section className="roadmap-state-card">
            <div className="roadmap-state-title">No upcoming enhancements are labeled yet</div>
            <div className="roadmap-state-copy">
              Add open GitHub issues with the <code>enhancement</code> label and they will show up
              here.
            </div>
          </section>
        )
      ) : null}
    </div>
  );
}
