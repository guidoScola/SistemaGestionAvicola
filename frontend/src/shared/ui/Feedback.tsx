type FeedbackProps = {
  error: string | null
  success: string | null
}

/** Shows the outcome of the last action: errors as alerts, successes as polite status messages. */
export function Feedback({ error, success }: FeedbackProps) {
  if (error) {
    return (
      <p role="alert" className="error">
        {error}
      </p>
    )
  }
  if (success) {
    return (
      <p role="status" className="success">
        {success}
      </p>
    )
  }
  return null
}
